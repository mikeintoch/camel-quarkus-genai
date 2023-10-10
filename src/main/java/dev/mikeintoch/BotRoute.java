package dev.mikeintoch;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;

import dev.mikeintoch.bean.Command;
import dev.mikeintoch.bean.Prompt;

public class BotRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("netty-http:http://0.0.0.0:8080/camel")
        .log("webhook message : ${body}")
        .to("direct:transform");

        from("webhook:telegram:bots")
                .log("Telegram Received: ${body}");

        from("direct:transform")
                .transform().jsonpath("$.message.text")
                .bean(this, "transformMessage")
                .marshal().json()
                .log("Message Transformed: ${body}")
        .to("direct:invokeGenAi");

        from("direct:invokeGenAi")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader("Accept", constant("application/json"))
                .setHeader("CamelHttpQuery", simple("key={{genai.api-key}}"))
                //.removeHeader(Exchange.HTTP_PATH)
        .to("https://generativelanguage.googleapis.com/v1beta2/models/text-bison-001:generateText?bridgeEndpoint=true")
        .log("GenAI Response: ${body}")
        .to("direct:toTelegram");

        from("direct:toTelegram")
                .transform().jsonpath("$.candidates[0].output")
        .to("telegram:bots?chatId={{telegram.chatId}}");
    }

    public void transformMessage(Exchange exchange) {
        Message in = exchange.getIn();

        Prompt prompt = new Prompt();
        Command command = new Command();

        prompt.setText(in.getBody(String.class));

        command.setPrompt(prompt);

        in.setBody(command);
    }
}
