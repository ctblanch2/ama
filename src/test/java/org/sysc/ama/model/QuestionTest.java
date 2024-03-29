package org.sysc.ama.model;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;


public class QuestionTest {

    private User author;
    private User amaSubject;
    private Ama  ama;

    @Before
    public void setUp () {
        this.author     = new User();
        this.amaSubject = new User();
        this.ama        = new Ama("Foo", this.amaSubject, true);
    }

    @Test
    public void testCreateQuestion () {
        String body = "How is the weather?";

        // For notes on timing, see comment in PostTest.testCreatePost
        Date beforeCreated = new Date(new Date().getTime() - 5);
        Date afterCreated = new Date(new Date().getTime() + 5);

        Question q = new Question(this.author, this.ama, body);

        assertEquals(q.getAuthor(), this.author);
        assertEquals(q.getAma(), this.ama);
        assertEquals(q.getBody(), body);
        assertFalse(q.isEdited());

        assertTrue(q.getCreated() instanceof Date);
        assertTrue(beforeCreated.before(q.getCreated()));
        assertTrue(afterCreated.after(q.getCreated()));
    }

}
