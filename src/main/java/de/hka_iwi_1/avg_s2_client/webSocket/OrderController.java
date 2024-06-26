/*
 * Copyright (c) 2024 - present Florian Sauer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the “Software”), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package de.hka_iwi_1.avg_s2_client.webSocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hka_iwi_1.avg_s2_client.entity.*;
import de.hka_iwi_1.avg_s2_client.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import static de.hka_iwi_1.avg_s2_client.webSocket.StockPriceController.exchange;

/**
 * Class for handling incoming orders.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    /**
     * Prefix for web socket API.
     */
    public static final String orderPrefix = "/order";

    /**
     * Prefix for web socket API.
     */
    public static final String receiveOrders = "/receiveOrders";

    private final OrderService orderService;

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final ObjectMapper mapper;

    /**
     * Receive buy orders from WebSocket clients.
     *
     * @param orderWrapper The order wrapper containing a buy- or sell-order. Used a wrapper for convenient
     *                     transportation and type handling.
     * @throws JsonProcessingException Exception thrown if Jackson fails to convert the string into an object.
     */
    @MessageMapping("/buy")
    public void sendBuyOrder(final OrderWrapper orderWrapper) throws JsonProcessingException {
        log.debug("sendBuyOrder: orderWrapper={}", orderWrapper);
        sendOrder(orderWrapper);
        publishOrders();
    }

    /**
     * Receive sell orders from WebSocket clients.
     *
     * @param orderWrapper The order wrapper containing a buy- or sell-order. Used a wrapper for convenient
     *                     transportation and type handling.
     * @throws JsonProcessingException Exception thrown if Jackson fails to convert the string into an object.
     */
    @MessageMapping("/sell")
    public void sendSellOrder(final OrderWrapper orderWrapper) throws JsonProcessingException {
        log.debug("sendSellOrder: orderWrapper={}", orderWrapper);
        sendOrder(orderWrapper);
        publishOrders();
    }

    private void sendOrder(OrderWrapper orderWrapper) throws JsonProcessingException {
        log.debug("sendOrder: orderWrapper={}", orderWrapper);
        orderService.sendOrder(orderWrapper);
        publishOrders();
    }

    @JmsListener(destination = "${jms.stocks.orderStatus.Stuttgart}")
    @JmsListener(destination = "${jms.stocks.orderStatus.Frankfurt}")
    private void receiveOrderStatus(String orderWrapperString) throws JsonProcessingException {
        log.debug("receiveOrderStatus: orderWrapperString={}", orderWrapperString);
        var orderWrapper = mapper.readValue(orderWrapperString, OrderWrapper.class);
        orderService.updateOrderStatus(orderWrapper);
        publishOrders();
    }

    /**
     * Receive requests for publishing all orders to WebsSocket clients.
     */
    @MessageMapping("/all")
    public void publishOrders() {
        log.debug("publishOrders");
        simpMessagingTemplate.convertAndSend(
                exchange + receiveOrders,
                orderService.getAll()
        );
    }
}
