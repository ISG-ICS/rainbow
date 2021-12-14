package controllers;

import actor.Agent;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;
import play.api.http.websocket.CloseCodes;
import play.http.websocket.Message;
import play.libs.F;
import play.libs.Scala;
import play.mvc.*;
import play.libs.streams.ActorFlow;
import akka.actor.*;
import akka.stream.*;
import javax.inject.Inject;
import com.typesafe.config.Config;
import scala.compat.java8.FutureConverters;

import java.util.concurrent.CompletionStage;
import static akka.pattern.Patterns.ask;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {

    private final Config config;
    private final ActorSystem actorSystem;
    private final Materializer materializer;

    @Inject
    public HomeController(Config config, ActorSystem actorSystem, Materializer materializer) {
        this.config = config;
        this.actorSystem = actorSystem;
        this.materializer = materializer;
    }

    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */
    public Result index() {
        return ok(views.html.index.render());
    }

//    public WebSocket ws() {
//        return WebSocket.Json.accept(
//                request -> ActorFlow.actorRef((actorRef) -> Agent.props(actorRef, config), actorSystem, materializer));
//    }

    public WebSocket ws() {
        return new WebSocket.MappedWebSocketAcceptor<>(
            Scala.partialFunction(
                inMessage -> {
                    try {
                        if (inMessage instanceof Message.Binary) {
                            return F.Either.Left(
                                    play.libs.Json.parse(
                                            ((Message.Binary) inMessage).data().iterator().asInputStream()));
                        } else if (inMessage instanceof Message.Text) {
                            return F.Either.Left(play.libs.Json.parse(((Message.Text) inMessage).data()));
                        }
                    } catch (RuntimeException e) {
                        return F.Either.Right(
                                new Message.Close(CloseCodes.Unacceptable(), "Unable to parse JSON message"));
                    }
                    throw Scala.noMatch();
                }
            ),
            outMessage -> {
                if (outMessage instanceof JsonNode) {
                    return new Message.Text(play.libs.Json.stringify((JsonNode) outMessage));
                }
                if (outMessage instanceof ByteString) {
                    return new Message.Binary((ByteString)outMessage);
                }
                throw Scala.noMatch();
            }
        ).accept(request -> ActorFlow.actorRef((actorRef) -> Agent.props(actorRef, config), actorSystem, materializer));
    }

    public CompletionStage<Result> transfer() {
        return FutureConverters.toJava(ask(actorSystem.actorOf(Agent.getProps()), JsonNode.class, 30000))
                .thenApply(response -> ok((JsonNode) response));
    }
}
