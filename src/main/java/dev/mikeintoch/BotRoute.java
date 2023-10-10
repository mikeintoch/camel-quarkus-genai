package dev.mikeintoch;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;

import dev.mikeintoch.bean.Command;
import dev.mikeintoch.bean.Prompt;

public class BotRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("telegram:bots")
                .to("direct:transform");

        from("direct:transform")
                .transform().jsonpath("$.text")
                .bean(this, "transformMessage")
                .marshal().json()
        .to("direct:invokeGenAi");

        from("direct:invokeGenAi")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader("Accept", constant("application/json"))
                .setHeader("CamelHttpQuery", simple("key={{genai.api-key}}"))
        .to("{{genai.api-url}}?bridgeEndpoint=true")
        .to("direct:toTelegram");

        from("direct:toTelegram")
                .transform().jsonpath("$.candidates[0].output")
        .to("telegram:bots");
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
