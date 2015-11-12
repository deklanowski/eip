package org.deklanowski.eip.routing.aggregator.internal;

import org.deklanowski.eip.camel.CamelMessagePublisher;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.deklanowski.eip.spi.Transformer;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AggregatingRouterIntegerToStringTest extends CamelTestSupport
{
    private static final  String DIRECT_START = "direct:start";
    private static final  String MOCK_RESULT = "mock:result";
    public static final String TEST_ROUTE = "test.route";

    @EndpointInject(uri = MOCK_RESULT)
    private MockEndpoint result;

    private Transformer<Integer,String> transformer = new Transformer<Integer, String>() {
        @Override
        public String transform(Integer input) {
            return String.valueOf(input);
        }
    };


    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        ProducerTemplate producer = context.createProducerTemplate();
        producer.setDefaultEndpointUri(MOCK_RESULT);

        return new AggregatingRouter(
                TEST_ROUTE,
                DIRECT_START,
                transformer,
                "${in.header.id} == null",
                1,
                1000L,
                new CamelMessagePublisher(producer),
                "log:DLQ");
    }


    @org.junit.Test
    public void testRoute() throws InterruptedException {
        result.expectedMessageCount(1);
        template.sendBody(DIRECT_START,1);
        result.assertIsSatisfied();
    }

}
