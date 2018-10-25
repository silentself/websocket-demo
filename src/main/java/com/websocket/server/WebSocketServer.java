package com.websocket.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

//@ServerEndpoint("/websocket/{user}")
@ServerEndpoint("/websocket")
@Component
public class WebSocketServer {
	private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);
	// 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
	private static int onlineCount = 0;
	// concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
	private static CopyOnWriteArraySet<WebSocketServer> webSocketSet = new CopyOnWriteArraySet<WebSocketServer>();
	// 与某个客户端的连接会话，需要通过它来给客户端发送数据
	private Session session;

	/**
	 * 连接建立成功调用的方法
	 */
	@OnOpen
	public void onOpen(Session session) {
		this.session = session;
		// 加入set中
		webSocketSet.add(this);
		// 在线数加1
		addOnlineCount();
		log.info("有新连接加入！当前在线人数为" + getOnlineCount());
		try {
			sendMessage("");
		} catch (IOException e) {
			log.error("case by :" + e.getMessage());
		}
		try {
			sendMessage("系统消息：连接成功！");
		} catch (IOException e) {
			log.error("websocket IO异常");
		}
	}
	// //连接打开时执行
	// @OnOpen
	// public void onOpen(@PathParam("user") String user, Session session) {
	// currentUser = user;
	// System.out.println("Connected ... " + session.getId());
	// }

	/**
	 * 连接关闭调用的方法
	 */
	@OnClose
	public void onClose() {
		webSocketSet.remove(this); // 从set中删除
		subOnlineCount(); // 在线数减1
		try {
			sendMessage("");
		} catch (IOException e) {
			log.error("case by :" + e.getMessage());
		}
		log.info("有一连接关闭！当前在线人数为" + getOnlineCount());
	}

	/**
	 * 收到客户端消息后调用的方法
	 *
	 * @param message
	 *            客户端发送过来的消息
	 */
	@OnMessage
	public void onMessage(String message, Session session) {
		log.info("来自客户端的消息:" + message);
		// 群发消息
		for (WebSocketServer item : webSocketSet) {
			try {
				item.sendMessage(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param session
	 * @param error
	 */
	@OnError
	public void onError(Session session, Throwable error) {
		log.error("发生错误");
		error.printStackTrace();
	}

	private static Gson gson = new Gson();

	public void sendMessage(String message) throws IOException {
		HashMap<Object, Object> hashMap = new HashMap<>();
		hashMap.put("data", message);
		hashMap.put("user_count", getOnlineCount());
		String json = gson.toJson(hashMap);
		this.session.getBasicRemote().sendText(json);
	}

	/**
	 * 群发自定义消息
	 */
	public static void sendInfo(String message) {
		log.info(message);
		for (WebSocketServer item : webSocketSet) {
			try {
				item.sendMessage(message);
			} catch (IOException ignored) {
			}
		}
	}

	private static synchronized int getOnlineCount() {
		return onlineCount;
	}

	private static synchronized void addOnlineCount() {
		WebSocketServer.onlineCount++;

	}

	private static synchronized void subOnlineCount() {
		WebSocketServer.onlineCount--;

	}
}
