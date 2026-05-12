package brettdansmith.drugdiary.domain.assistant;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AssistantCommandParserTest {
    @Test
    public void knownCommandsMatchRegisteredSurface() {
        assertTrue(AssistantCommandParser.isKnownCommand("/help"));
        assertTrue(AssistantCommandParser.isKnownCommand("/commands"));
        assertTrue(AssistantCommandParser.isKnownCommand("/help drugdata"));
        assertTrue(AssistantCommandParser.isKnownCommand("/?"));
        assertTrue(AssistantCommandParser.isKnownCommand("/context"));
        assertTrue(AssistantCommandParser.isKnownCommand("/placeholders"));
        assertTrue(AssistantCommandParser.isKnownCommand("/settings"));
        assertTrue(AssistantCommandParser.isKnownCommand("/privacy"));
        assertTrue(AssistantCommandParser.isKnownCommand("/logs recent"));
        assertTrue(AssistantCommandParser.isKnownCommand("/reminders active"));
        assertTrue(AssistantCommandParser.isKnownCommand("/newchat Notes"));
        assertTrue(AssistantCommandParser.isKnownCommand("/drugcache clear"));
        assertTrue(AssistantCommandParser.isKnownCommand("/drugcache status"));
        assertTrue(AssistantCommandParser.isKnownCommand("/drugcache warm moclobemide"));
        assertTrue(AssistantCommandParser.isKnownCommand("/drugcache warm common"));
        assertTrue(AssistantCommandParser.isKnownCommand("/drugcache warm recreational"));
        assertTrue(AssistantCommandParser.isKnownCommand("/pubchem caffeine"));
        assertTrue(AssistantCommandParser.isKnownCommand("/interact saved"));
        assertTrue(AssistantCommandParser.isKnownCommand("/alias aurorix"));
        assertTrue(AssistantCommandParser.isKnownCommand("/dose moclobemide"));
        assertTrue(AssistantCommandParser.isKnownCommand("/meds favorites"));
        assertTrue(AssistantCommandParser.isKnownCommand("/updatemed aspirin | strength=100 mg"));
        assertTrue(AssistantCommandParser.isKnownCommand("/editmed aspirin | form=capsule"));
        assertTrue(AssistantCommandParser.isKnownCommand("/removemed aspirin"));
        assertTrue(AssistantCommandParser.isKnownCommand("/deletemed aspirin"));
        assertTrue(AssistantCommandParser.isKnownCommand("/delmed aspirin"));
        assertTrue(AssistantCommandParser.isKnownCommand("/clearmeds"));
        assertTrue(AssistantCommandParser.isKnownCommand("/importmeds Medical"));
        assertTrue(AssistantCommandParser.isKnownCommand("/importmeds\nMedical\n• Paracetamol"));

        assertFalse(AssistantCommandParser.isKnownCommand("/pubchem"));
        assertFalse(AssistantCommandParser.isKnownCommand("/notacommand caffeine"));
    }

    @Test
    public void quotedAssistantSuggestionsHaveExactClickableBounds() {
        String text = "Try \"/rxnorm ibuprofen\" or \"/openfda raw aspirin\" next.";
        List<AssistantCommandParser.Span> spans = AssistantCommandParser.findCommands(text);

        assertEquals(2, spans.size());
        assertEquals("/rxnorm ibuprofen", spans.get(0).command);
        assertEquals("/rxnorm ibuprofen", text.substring(spans.get(0).start, spans.get(0).end));
        assertEquals("/openfda raw aspirin", spans.get(1).command);
        assertEquals("/openfda raw aspirin", text.substring(spans.get(1).start, spans.get(1).end));
    }

    @Test
    public void bareInlineSlashTextIsHighlightedWhenRecognized() {
        String text = "This is not a suggestion /rxnorm ibuprofen because it is not quoted.";
        List<AssistantCommandParser.Span> spans = AssistantCommandParser.findCommands(text);
        assertEquals(1, spans.size());
        assertEquals("/rxnorm ibuprofen", spans.get(0).command);
        assertEquals("/rxnorm ibuprofen", text.substring(spans.get(0).start, spans.get(0).end));
    }

    @Test
    public void lineStartCommandStillHighlightsForTypedCommands() {
        String text = "/drugdata caffeine";
        List<AssistantCommandParser.Span> spans = AssistantCommandParser.findCommands(text);

        assertEquals(1, spans.size());
        assertEquals("/drugdata caffeine", spans.get(0).command);
    }

    @Test
    public void inlineCommandTrimsTrailingSentenceWords() {
        String text = "Use /meds to view current medications.";
        List<AssistantCommandParser.Span> spans = AssistantCommandParser.findCommands(text);

        assertEquals(1, spans.size());
        assertEquals("/meds", spans.get(0).command);
        assertEquals("/meds", text.substring(spans.get(0).start, spans.get(0).end));
    }
}
