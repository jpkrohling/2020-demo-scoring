package org.rhdemo.scoring;

import org.rhdemo.scoring.models.CurrentRound;
import org.rhdemo.scoring.models.Environment;
import org.rhdemo.scoring.models.GameStatus;
import org.rhdemo.scoring.models.Round;
import org.rhdemo.scoring.models.Score;
import org.rhdemo.scoring.services.GamingService;
import org.rhdemo.scoring.services.KafkaLeaderboard;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/game")
public class ScoringResource {
    public static final int ROUND_POINTS = 100;
    public static final int WRONG_GUESS = 5;

    @Inject
    GamingService games;

    @Inject
    KafkaLeaderboard leaderboard;

    @Inject
    Environment env;

    @Path("/join")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public GameStatus join(GameStatus state) {
        Round round = games.firstRound(state.getGame());
        CurrentRound current = new CurrentRound(round);
        current.setPointsAvailable(ROUND_POINTS);
        Score score = new Score();
        score.setScoringServer(env.CLUSTER_NAME());
        score.setStatus(Score.START);
        state.setScore(score);
        state.setCurrentRound(current);
        return state;
    }


    private void error(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("message", message);
        throw new WebApplicationException(Response.status(400)
                .type(MediaType.APPLICATION_JSON)
                .entity(error).build());

    }

    @Path("/score")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public GameStatus scoring(GameStatus state) {
        CurrentRound currentRound = state.getCurrentRound();
        Round round = games.getRound(state.getGame(), currentRound.getId());
        state.getScore().setScoringServer(env.CLUSTER_NAME());


        boolean failed = false;
        boolean allGuessesCorrect = true;
        for (int i = 0; i < currentRound.getGuess().size(); i++) {
            Object curr = currentRound.getGuess().get(i);
            if (curr instanceof Integer) {
                if (!round.getPrice().get(i).equals(curr)) {
                    failed = true;
                    currentRound.getGuess().set(i, ""); // reset guess to empty slot
                    allGuessesCorrect = false;
                }
            } else if (".".equals(curr)) {
                if (!round.getPrice().get(i).equals(curr)) {
                    failed = true;
                    allGuessesCorrect = false;
                }
            } else {
                // index not guessed yet
                allGuessesCorrect = false;
            }
        }
        if (allGuessesCorrect && currentRound.getGuess().size() != round.getPrice().size()) allGuessesCorrect = false;


        // update or reset choices
        for (int i = 0; i < currentRound.getChoices().size(); i++) {
            Object choice = currentRound.getChoices().get(i);
            if ("correct".equals(choice)) continue;
            if ("guess".equals(choice)) {
                if (failed) {
                    currentRound.getChoices().set(i, round.getChoices().get(i));
                } else {
                    currentRound.getChoices().set(i, "correct");
                }
            }
        }

        if (failed) {
            failedGuess(state);
        } else {
            correctGuess(state, allGuessesCorrect);
        }
        leaderboard.send(state);
        return state;
    }

    private void correctGuess(GameStatus state, boolean allCorrect) {
        state.getScore().setRight(state.getScore().getRight() + 1);
        if (allCorrect) { // COMPLETE!
            state.getScore().setScore(state.getScore().getScore() + state.getCurrentRound().getPointsAvailable());
            state.getScore().setAward(state.getCurrentRound().getPointsAvailable());
            state.getScore().setStatus(Score.COMPLETED_ROUND);
            // next round
            Round next = games.nextRound(state.getGame(), state.getCurrentRound().getId());
            if (next == null) {
                state.setCurrentRound(null);
                state.getScore().setStatus(Score.GAME_OVER);
            } else {
                CurrentRound nextCurrent = new CurrentRound(next);
                nextCurrent.setPointsAvailable(ROUND_POINTS);
                state.setCurrentRound(nextCurrent);
            }
        } else {
            state.getScore().setStatus(Score.CORRECT_GUESS);
        }
    }

    private void failedGuess(GameStatus state) {
        state.getScore().setStatus(Score.BAD_GUESS);
        state.getScore().setWrong(state.getScore().getWrong() + 1);
        int pointsAvailable = state.getCurrentRound().getPointsAvailable();
        pointsAvailable -= WRONG_GUESS;
        if (pointsAvailable <= 0) pointsAvailable = 1;
        state.getCurrentRound().setPointsAvailable(pointsAvailable);
    }
}