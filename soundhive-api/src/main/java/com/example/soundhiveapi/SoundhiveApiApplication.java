package com.example.soundhiveapi;

import com.example.soundhiveapi.model.Tag;
import com.example.soundhiveapi.model.User;
import com.example.soundhiveapi.neural.ModelSerializer;
import com.example.soundhiveapi.neural.NeuralNetwork;
import com.example.soundhiveapi.service.MyJdbcService;
import com.example.soundhiveapi.service.TrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.PriorityQueue;

@SpringBootApplication
public class SoundhiveApiApplication implements CommandLineRunner {

	@Autowired private MyJdbcService    myJdbcService;
	@Autowired private NeuralNetwork    neuralNetwork;
	@Autowired private TrainingService  trainingService;

	private static final int TOP_N = 5;

	public static void main(String[] args) {
		SpringApplication.run(SoundhiveApiApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// load existing NN parameters
		ModelSerializer.loadModel(neuralNetwork);

		// fine‑tune once on startup
		trainingService.train();

		// demo: fetch a user and print their top tags
		String testEmail = "mark.wilson34@gmail.com";
		User user = myJdbcService.getUserByEmail(testEmail);
		if (user != null)
			System.out.println("User Found: " + user.getUsername());
		else
			System.out.println("User Not Found");;

//		List<Tag> allTags = myJdbcService.getAllTags();
//		double[] vec = myJdbcService.getUserTagWeightsArray(user.getIdNumber());
//		for (int i = 0; i < allTags.size(); i++) {
//			System.out.println(
//					allTags.get(i).getTagName()
//							+ " (ID:" + allTags.get(i).getTagId() + "): "
//							+ vec[i]
//			);
//		}
//
//		PriorityQueue<MyJdbcService.TagWeight> maxHeap =
//				myJdbcService.getUserTagMaxHeap(
//						user.getIdNumber(),
//						myJdbcService.getUserTagWeightsMatrix(),
//						myJdbcService.getDistinctUserIds(),
//						allTags
//				);
//
//		System.out.println("Top " + TOP_N + " tags for user " + user.getUsername() + ":");
//		for (int i = 0; i < TOP_N && !maxHeap.isEmpty(); i++) {
//			System.out.println(maxHeap.poll());
//		}
	}
}
