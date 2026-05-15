package brettdansmith.drugdiary.domain.model.diary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import brettdansmith.drugdiary.domain.model.diary.DiaryEntry;
import brettdansmith.drugdiary.domain.model.diary.MoodCheckIn;
import brettdansmith.drugdiary.domain.model.diary.SleepLog;
import brettdansmith.drugdiary.domain.model.diary.SymptomLog;

public class DiaryEntryTest {
    @Test
    public void moodAndSleepClampIntoExpectedRange() {
        MoodCheckIn mood = new MoodCheckIn(15, -2, 7, 11);
        SleepLog sleep = new SleepLog(7.5, 19);
        SymptomLog symptom = new SymptomLog("Headache", -1);

        assertEquals(10, mood.mood);
        assertEquals(0, mood.anxiety);
        assertEquals(10, mood.energy);
        assertEquals(10, sleep.quality);
        assertEquals(0, symptom.severity);
    }

    @Test
    public void diaryEntryDefaultsWhenOptionalFieldsMissing() {
        DiaryEntry entry = new DiaryEntry("", "Mood check", "", null, null, null, "", null, 0);
        assertTrue(entry.id.startsWith("diary_"));
        assertEquals("Mood check", entry.title);
        assertEquals(0, entry.mood.mood);
    }
}
