package com.saasovation.agilepm.infra.exchange;

import com.saasovation.agilepm.infra.exchange.forum.adapters.DiscussionStartedAdapter;
import com.saasovation.agilepm.infra.exchange.forum.model.DiscussionStarted;
import com.saasovation.agilepm.infra.exchange.forum.receivers.DiscussionStartedReceiver;
import com.saasovation.agilepm.infra.exchange.product.adapters.ProductDiscussionRequestedEventAdapter;
import com.saasovation.agilepm.model.product.Events.ProductDiscussionRequested;
import io.vlingo.actors.Stage;
import io.vlingo.actors.World;
import io.vlingo.lattice.exchange.ExchangeSender;
import io.vlingo.lattice.exchange.camel.CamelExchange;
import io.vlingo.lattice.exchange.camel.CoveyFactory;
import io.vlingo.lattice.exchange.camel.sender.ExchangeSenders;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.engine.DefaultConsumerTemplate;
import org.apache.camel.impl.engine.DefaultProducerTemplate;
import org.apache.camel.support.DefaultRegistry;

public class ExchangeBootstrap {

    private final Stage stage;

    public ExchangeBootstrap(final World world) throws Exception {
        stage = world.stage();
    }

    @SuppressWarnings("resource")
    public io.vlingo.lattice.exchange.Exchange initExchange() throws Exception {
        DefaultCamelContext camelContext = new DefaultCamelContext(new DefaultRegistry());
        camelContext.start();

        DefaultProducerTemplate producerTemplate = new DefaultProducerTemplate(camelContext);
        DefaultConsumerTemplate consumerTemplate = new DefaultConsumerTemplate(camelContext);

        producerTemplate.start();
        consumerTemplate.start();


        final String exchangeUri = "rabbitmq:agile-iddd-product?hostname=localhost&portNumber=5672";

        final io.vlingo.lattice.exchange.Exchange camelExchange = new CamelExchange(camelContext, "agilepm-exchange", exchangeUri);
        final ExchangeSender<Exchange> sender = ExchangeSenders.sendingTo(exchangeUri, camelContext);

        camelExchange.register(CoveyFactory.build(sender,
                new NoOpReceiver<>(),
                new ProductDiscussionRequestedEventAdapter(camelContext),
                ProductDiscussionRequested.class,
                ProductDiscussionRequested.class
        ));

        camelExchange.register(CoveyFactory.build(sender,
                new DiscussionStartedReceiver(this.stage),
                new DiscussionStartedAdapter(camelContext),
                DiscussionStarted.class,
                DiscussionStarted.class
        ));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            producerTemplate.stop();
            consumerTemplate.stop();
            camelExchange.close();

            System.out.println("\n");
            System.out.println("=======================");
            System.out.println("Stopping camel exchange.");
            System.out.println("=======================");
        }));

        return camelExchange;
    }


}
