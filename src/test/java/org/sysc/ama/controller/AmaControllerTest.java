package org.sysc.ama.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;


import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.annotation.DirtiesContext;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.sysc.ama.model.Question;
import org.sysc.ama.model.User;
import org.sysc.ama.model.Ama;

import org.sysc.ama.repo.QuestionRepository;
import org.sysc.ama.repo.UserRepository;
import org.sysc.ama.repo.AmaRepository;

import javax.annotation.PostConstruct;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AmaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AmaRepository amaRepo;

    @Autowired
    private QuestionRepository questionRepo;

    private User testUser;

    private Ama amaFoo;
    private Ama amaBar;
    private Ama amaBaz;

    private Question fooQuestion;

    @PostConstruct
    public void init() {
        this.testUser = new User("TestUser");
        this.userRepo.save(this.testUser);

        this.userRepo.save(new User("BadUser"));
    }

    @Before
    public void before () {
        // Note About Delay
        //
        // When an AMA is created it receives a created timestamp that is accurate to the
        // nearest millisecond. In order to properly test sorting by time, an artificial
        // delay of 2 milliseconds has been added between the creation of these test AMAs.
        // This will guarantee the order of the AMAs when sorting by created dates.

        this.amaFoo = new Ama("Foo", this.testUser, true);
        delay(2);
        this.amaBar = new Ama("Bar", this.testUser, true);
        delay(2);
        this.amaBaz = new Ama("Baz", this.testUser, true);

        amaRepo.save(this.amaFoo);
        amaRepo.save(this.amaBar);
        amaRepo.save(this.amaBaz);
        this.fooQuestion = new Question(this.testUser, this.amaFoo, "Don't avoid the question");
        questionRepo.save(this.fooQuestion);
    }

    @Test
    @WithUserDetails("TestUser")
    public void testCreateEndpointExists () throws Exception {
        mockMvc.perform(post("/ama?title=Foo&public=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.public").value("true"))
                .andExpect(jsonPath("$.title").value("Foo"))
                .andExpect(jsonPath(  "$.id").isNumber());

    }

    @Test
    @WithMockUser("TestUser")
    public void testListAma () throws Exception {
        mockMvc.perform(get("/ama/list?page=0&limit=2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].title").value("Baz"))
            .andExpect(jsonPath("$[1].title").value("Bar"));
    }

    @Test
    @WithMockUser("TestUser")
    public void testListAmaPaging () throws Exception {
        mockMvc.perform(get("/ama/list?page=1&limit=2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("Foo"));
    }

    @Test
    @WithMockUser("TestUser")
    public void testListAmaOrdering () throws Exception {
        mockMvc.perform(get("/ama/list?page=0&limit=2&asc=true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].title").value("Foo"))
            .andExpect(jsonPath("$[1].title").value("Bar"));
    }

    @Test
    @WithMockUser("TestUser")
    public void testListAmaSorting () throws Exception {
        mockMvc.perform(get("/ama/list?page=0&limit=2&sort=title&asc=true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].title").value("Bar"))
            .andExpect(jsonPath("$[1].title").value("Baz"));
    }


    @Test
    @WithMockUser("TestUser")
    public void testDeleteExistingAma () throws Exception {
        mockMvc.perform(delete("/ama/" + this.amaFoo.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Foo"));
    }

    @Test
    @WithMockUser("TestUser")
    public void testDeleteAmaDoesNotExist () throws Exception {
        mockMvc.perform(delete("/ama/100"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser("TestUser")
    public void testAddQuestionToAma () throws Exception {

        mockMvc.perform(post("/ama/" + this.amaFoo.getId() + "/question")
                            .param("body", "What is the meaning of life?")
                            .param("userId", this.testUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("What is the meaning of life?"))
                .andExpect(jsonPath("$.ama.id").value(this.amaFoo.getId()));
    }


    @Test
    @WithMockUser("TestUser")
    public void testViewAma () throws Exception {

        mockMvc.perform(get("/ama/" + this.amaFoo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(this.amaFoo.getId()));
    }

    @Test
    @WithMockUser("TestUser")
    public void testViewQuestions () throws Exception {

        mockMvc.perform(post("/ama/" + this.amaFoo.getId() + "/question")
                .param("body", "What is the meaning of life?")
                .param("userId", this.testUser.getId().toString()))
                .andReturn();

        mockMvc.perform(post("/ama/" + this.amaFoo.getId() + "/question")
                .param("body", "Is that a hippo?")
                .param("userId", this.testUser.getId().toString()))
                .andReturn();

        mockMvc.perform(get("/ama/" + this.amaFoo.getId() + "/questions")
                            .param("page", "0")
                            .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.body == \"Is that a hippo?\")]").exists())
                .andExpect(jsonPath("$[?(@.body == \"What is the meaning of life?\")]").exists())
                .andReturn();
    }

    @Test
    @WithMockUser("TestUser")
    public void testViewQuestion () throws Exception {

        mockMvc.perform(post("/ama/" + this.amaFoo.getId() + "/question")
                .param("body", "What is the meaning of life?")
                .param("userId", this.testUser.getId().toString()));

        mockMvc.perform(get("/ama/" + this.amaFoo.getId() + "/question/" + this.fooQuestion.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Don't avoid the question"));
    }

    @Test
    @WithMockUser("TestUser")
    public void testDeleteQuestion () throws Exception {

        MvcResult result =  mockMvc.perform(post("/ama/" + this.amaFoo.getId() + "/question")
                .param("body", "What is the meaning of life?")
                .param("userId", this.testUser.getId().toString()))
                .andReturn();

        Integer id = JsonPath.parse(result.getResponse().getContentAsString()).read("$.id");

        mockMvc.perform(delete("/ama/" + this.amaFoo.getId() + "/question/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("What is the meaning of life?"));

        mockMvc.perform(get("/ama/" + this.amaFoo.getId() + "/questions")
                .param("page", "0")
                .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.body == \"What is the meaning of life?\")]").doesNotExist());
    }

    @Test
    @WithUserDetails("TestUser")
    public void testAddAnswer () throws Exception {
        MvcResult result = mockMvc.perform(post("/ama/" + this.amaFoo.getId() + "/question")
                .param("body", "What is the meaning of life?")
                .param("userId", this.testUser.getId().toString()))
                .andReturn();
        Integer questionId = JsonPath.parse(result.getResponse().getContentAsString()).read("$.id");

        mockMvc.perform(post("/ama/" + this.amaFoo.getId() + "/question/" + questionId + "/answer")
                .param("body", "No clue"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.body").value("No clue"));
    }

    @Test
    @WithUserDetails("BadUser")
    public void testOnlyAmaSubjectCanAnswerQuestion () throws Exception {
        mockMvc.perform(post("/ama/" + this.amaFoo.getId() + "/question/" + this.fooQuestion.getId() + "/answer")
                .param("body", "No clue"))
                .andExpect(status().isForbidden());
    }


    /**
     * Sleeps the current process for the given number of milliseconds
     *
     * @param time - The duration to sleep the process
     */
    public void delay (int time) {
        try {
            Thread.sleep(time);
        } catch(InterruptedException ex) {}
    }
}
