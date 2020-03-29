package com.hongfei.qi.nativeribbon;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.LoadBalancerBuilder;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import com.netflix.loadbalancer.reactive.ServerOperation;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import rx.Observable;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class NativeRibbonApplication implements ApplicationRunner {

	public static void main(String[] args) {
		SpringApplication.run(NativeRibbonApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		System.out.println(Thread.currentThread().getName());
		List<Server> serverList = new ArrayList<>();
		serverList.add(new Server("localhost",5551));
		serverList.add(new Server("localhost",5550));

		BaseLoadBalancer baseLoadBalancer = LoadBalancerBuilder.newBuilder()
				.buildFixedServerListLoadBalancer(serverList);
		for(int i=0; i<5; i++){
			System.out.println("-----------------");
			Observable<Object> submit = LoadBalancerCommand.builder()
					.withLoadBalancer(baseLoadBalancer)
					.build()
					.submit(
							new ServerOperation<Object>() {
								@Override
								public Observable<Object> call(Server server) {
									System.out.println("a from " + Thread.currentThread().getName());
									String addr = "http://" + server.getHost() + ":" + server.getPort() + "/name";
									System.out.println("invoke address = " + addr);
									URL url = null;
									try {
										url = new URL(addr);
									} catch (MalformedURLException e) {
										e.printStackTrace();
									}
									URLConnection urlConnection = null;
									try {
										urlConnection = url.openConnection();
									} catch (IOException e) {
										e.printStackTrace();
									}
									try {
										urlConnection.connect();
									} catch (IOException e) {
										e.printStackTrace();
									}

									try {
										InputStream inputStream = urlConnection.getInputStream();
										byte[] data = new byte[inputStream.available()];
										inputStream.read(data);
										return Observable.just(new String(data));
									} catch (IOException e) {
										e.printStackTrace();
										return Observable.error(e);
									}


								}
							}
					);
			Object first = submit.toBlocking().first();
			System.out.println("result = "+first);
		}



	}
}
