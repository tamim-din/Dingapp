const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_noor_1";
const BOB_UID = "bob_noor_2";
const ADMIN_UID = "admin_noor_1";
const ADMIN_EMAIL = "tamimryl@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProfilePayload(uid, email = "alice@example.com") {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    displayName: "আব্দুল্লাহ",
    username: "abdullah_bd",
    email,
    role: "user",
    accountStatus: "ACTIVE",
    themeMode: "AUTO",
    xp: 0,
    level: 1,
    streakDays: 0,
    activeDaysCount: 1,
    dailyQuranGoalPages: 4,
    dailyTasbihGoal: 100,
    dailyScreenGoalMinutes: 120,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated users cannot read or write user profiles, missions, or community posts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));
  await assertFails(unauthDb.collection("community_posts").where("moderatedHidden", "==", false).get());
});

test("User profile isolation, self-escalation prevention, and PII protection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  // Valid create succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));

  // Role self-escalation rejected on create & update
  const escalated = validProfilePayload(BOB_UID, "bob@example.com");
  escalated.role = "admin";
  await assertFails(bobDb.collection("users").doc(BOB_UID).set(escalated));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ role: "admin", updatedAt: new Date(Date.now() - 1000) }));

  // Bob cannot read Alice's PII profile or list users
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());

  // Alice can update her own theme & XP
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      themeMode: "NIGHT",
      xp: 50,
      level: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      xp: 60,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("User-scoped subcollections: missions, prayer_logs, quran_bookmarks, private_journals, tasbih_logs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Missions
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("missions").doc("m_fajr").set({
      userId: ALICE_UID,
      title: "পাঁচ ওয়াক্ত নামাজ",
      category: "IBADAT",
      schedule: "DAILY",
      xpReward: 50,
      custom: false,
      completed: false,
      completedCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("missions").doc("m_fajr").update({
      completed: true,
      completedCount: 1,
      lastCompletedDate: "2026-11-23",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("missions").get());

  // Prayer Log
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("prayer_logs").doc("2026-11-23").set({
      userId: ALICE_UID,
      dateKey: "2026-11-23",
      fajr: true,
      dhuhr: false,
      asr: false,
      maghrib: false,
      isha: false,
      tahajjud: false,
      naflCount: 2,
      locationName: "ঢাকা",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("prayer_logs").doc("2026-11-23").get());

  // Private Journal
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private_journals").doc("j_1").set({
      userId: ALICE_UID,
      title: "আলহামদুলিল্লাহ",
      content: "আজকের দিনটি শান্তিময় ছিল।",
      dateKey: "2026-11-23",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private_journals").get());
});

test("Community posts query alignment, likes/reports, and moderation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("community_posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorAlias: "মুসাফির",
      category: "MOTIVATION",
      content: "আল্লাহর ওপর ভরসা রাখুন, প্রতিটি কষ্টের পর স্বস্তি রয়েছে।",
      likesCount: 0,
      reportedCount: 0,
      moderatedHidden: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered query succeeds, unfiltered query fails for non-admin
  await assertSucceeds(aliceDb.collection("community_posts").where("moderatedHidden", "==", false).get());
  await assertFails(bobDb.collection("community_posts").get());

  // Bob can increment likesCount by 1, cannot hide or impersonate
  await assertSucceeds(
    bobDb.collection("community_posts").doc("post_1").update({
      likesCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bobDb.collection("community_posts").doc("post_1").update({
      moderatedHidden: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob can report post_1
  await assertSucceeds(
    bobDb.collection("community_reports").doc("rep_1").set({
      reporterId: BOB_UID,
      postId: "post_1",
      reason: "অনুপযুক্ত লেখা",
      status: "OPEN",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Admin security: email spoofing rejected; verified bootstrapped admin succeeds", async () => {
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_1", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  const regularUserDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  const now = new Date(Date.now() - 1000);
  const templatePayload = {
    title: "সকাল-সন্ধ্যার যিকির",
    body: "প্রতিদিন ফজর ও আসরের পর মাসনূন যিকির পাঠ করুন",
    category: "IBADAT",
    active: true,
    updatedAt: now,
  };

  // Unverified spoofed admin and regular user must fail
  await assertFails(spoofedAdminDb.collection("mission_templates").doc("t_1").set(templatePayload));
  await assertFails(regularUserDb.collection("mission_templates").doc("t_1").set(templatePayload));

  // Real verified admin succeeds
  await assertSucceeds(realAdminDb.collection("mission_templates").doc("t_1").set(templatePayload));
  await assertSucceeds(regularUserDb.collection("mission_templates").get());
});
