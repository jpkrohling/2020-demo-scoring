package org.rhdemo.scoring;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.rhdemo.scoring.models.Guess;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class ScoringResourceTest {

    static final String PLAYER = "{\n" +
            "    \"id\": \"1\",\n" +
            "    \"username\": \"Emerald Wanderer\",\n" +
            "    \"avatar\": {\n" +
            "      \"body\": 1,\n" +
            "      \"eyes\": 3,\n" +
            "      \"mouth\": 0,\n" +
            "      \"ears\": 2,\n" +
            "      \"nose\": 1,\n" +
            "      \"color\": 3\n" +
            "    }\n" +
            "}\n";

    @Test
    public void testEndpoint() throws Exception {
        InputStream is = given()
                .when().contentType("application/json").body(PLAYER).post("/game/join")
                .then()
                .statusCode(200)
                .body("creationServer", equalTo("SFO"))
                .body("gameServer", equalTo("SFO"))
                .body("scoringServer", equalTo("SFO"))
                .body("game.id", equalTo("1"))
                .body("game.state", equalTo("active"))
                .body("game.date", notNullValue())
                .body("player.id", equalTo("1"))
                .body("player.username", equalTo("Emerald Wanderer"))
                .body("score", equalTo(0))
                .body("right", equalTo(0))
                .body("wrong", equalTo(0))
                .body("currentRound.id", equalTo(0))
                .body("currentRound.pointsAvailable", equalTo(100))
                .body("currentRound.version", equalTo("1"))
                .body("currentRound.name", equalTo("Dollar bill"))
                .body("currentRound.choices[0]", equalTo(9))
                .body("currentRound.choices[1]", equalTo(1))
                .body("currentRound.choices[2]", equalTo(0))
                .body("currentRound.choices[3]", equalTo(5))
                .body("currentRound.choices[4]", equalTo(0))
                .body("currentRound.choices[5]", equalTo(1))
        .extract().asInputStream();

        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        ObjectReader guessReader = mapper.readerFor(Guess.class);
        ObjectWriter guessWriter = mapper.writerFor(Guess.class);
        Guess guess = guessReader.readValue(is);
        List<Object> guesses = new LinkedList<>();
        guesses.add((Integer)1);
        guesses.add(".");
        guess.getCurrentRound().setGuess(guesses);

        String json = guessWriter.writeValueAsString(guess);

        is = given().when().contentType("application/json").body(json).post("/game/score")
                .then()
                .statusCode(200)
                .body("creationServer", equalTo("SFO"))
                .body("gameServer", equalTo("SFO"))
                .body("scoringServer", equalTo("SFO"))
                .body("status", equalTo("CORRECT_GUESS"))
                .body("game.id", equalTo("1"))
                .body("game.state", equalTo("active"))
                .body("game.date", notNullValue())
                .body("player.id", equalTo("1"))
                .body("player.username", equalTo("Emerald Wanderer"))
                .body("score", equalTo(0))
                .body("right", equalTo(1))
                .body("wrong", equalTo(0))
                .body("currentRound.id", equalTo(0))
                .body("currentRound.pointsAvailable", equalTo(100))
                .body("currentRound.version", equalTo("1"))
                .body("currentRound.name", equalTo("Dollar bill"))
                .body("currentRound.choices[0]", equalTo(9))
                .body("currentRound.choices[1]", equalTo(0))
                .body("currentRound.choices[2]", equalTo(5))
                .body("currentRound.choices[3]", equalTo(0))
                .body("currentRound.choices[4]", equalTo(1))
                .extract().asInputStream();
        // wrong guess
        guess = guessReader.readValue(is);
        guesses = new LinkedList<>();
        guesses.add((Integer)1);
        guesses.add(".");
        guesses.add(9);
        guess.getCurrentRound().setGuess(guesses);

        json = guessWriter.writeValueAsString(guess);

        is = given().when().contentType("application/json").body(json).post("/game/score")
                .then()
                .statusCode(200)
                .body("creationServer", equalTo("SFO"))
                .body("gameServer", equalTo("SFO"))
                .body("scoringServer", equalTo("SFO"))
                .body("status", equalTo("BAD_GUESS"))
                .body("game.id", equalTo("1"))
                .body("game.state", equalTo("active"))
                .body("game.date", notNullValue())
                .body("player.id", equalTo("1"))
                .body("player.username", equalTo("Emerald Wanderer"))
                .body("score", equalTo(0))
                .body("right", equalTo(1))
                .body("wrong", equalTo(1))
                .body("currentRound.id", equalTo(0))
                .body("currentRound.pointsAvailable", equalTo(95))
                .body("currentRound.version", equalTo("1"))
                .body("currentRound.name", equalTo("Dollar bill"))
                .body("currentRound.choices[0]", equalTo(9))
                .body("currentRound.choices[1]", equalTo(0))
                .body("currentRound.choices[2]", equalTo(5))
                .body("currentRound.choices[3]", equalTo(0))
                .body("currentRound.choices[4]", equalTo(1))
                .extract().asInputStream();

        guess = guessReader.readValue(is);
        guesses = new LinkedList<>();
        guesses.add((Integer)1);
        guesses.add(".");
        guesses.add((Integer)0);
        guess.getCurrentRound().setGuess(guesses);

        json = guessWriter.writeValueAsString(guess);

        is = given().when().contentType("application/json").body(json).post("/game/score")
                .then()
                .statusCode(200)
                .body("creationServer", equalTo("SFO"))
                .body("gameServer", equalTo("SFO"))
                .body("scoringServer", equalTo("SFO"))
                .body("status", equalTo("CORRECT_GUESS"))
                .body("game.id", equalTo("1"))
                .body("game.state", equalTo("active"))
                .body("game.date", notNullValue())
                .body("player.id", equalTo("1"))
                .body("player.username", equalTo("Emerald Wanderer"))
                .body("score", equalTo(0))
                .body("right", equalTo(2))
                .body("wrong", equalTo(1))
                .body("currentRound.id", equalTo(0))
                .body("currentRound.pointsAvailable", equalTo(95))
                .body("currentRound.version", equalTo("1"))
                .body("currentRound.name", equalTo("Dollar bill"))
                .body("currentRound.choices[0]", equalTo(9))
                .body("currentRound.choices[1]", equalTo(5))
                .body("currentRound.choices[2]", equalTo(0))
                .body("currentRound.choices[3]", equalTo(1))
                .extract().asInputStream();

        // FINISHED

        guess = guessReader.readValue(is);
        guesses = new LinkedList<>();
        guesses.add((Integer)1);
        guesses.add(".");
        guesses.add((Integer)0);
        guesses.add((Integer)0);
        guess.getCurrentRound().setGuess(guesses);

        json = guessWriter.writeValueAsString(guess);

        is = given().when().contentType("application/json").body(json).post("/game/score")
                .then()
                .statusCode(200)
                .body("creationServer", equalTo("SFO"))
                .body("gameServer", equalTo("SFO"))
                .body("scoringServer", equalTo("SFO"))
                .body("status", equalTo("COMPLETED_ROUND"))
                .body("game.id", equalTo("1"))
                .body("game.state", equalTo("active"))
                .body("game.date", notNullValue())
                .body("player.id", equalTo("1"))
                .body("player.username", equalTo("Emerald Wanderer"))
                .body("score", equalTo(95))
                .body("award", equalTo(95))
                .body("right", equalTo(3))
                .body("wrong", equalTo(1))
                .body("currentRound.id", equalTo(1))
                .body("currentRound.pointsAvailable", equalTo(100))
                .body("currentRound.version", equalTo("1"))
                .body("currentRound.name", equalTo("Kernel of truth t-shirt"))
                .body("currentRound.choices[0]", equalTo(5))
                .body("currentRound.choices[1]", equalTo(4))
                .body("currentRound.choices[2]", equalTo(8))
                .body("currentRound.choices[3]", equalTo(5))
                .body("currentRound.choices[4]", equalTo(7))
                .body("currentRound.choices[5]", equalTo(8))
                .extract().asInputStream();

    }

}