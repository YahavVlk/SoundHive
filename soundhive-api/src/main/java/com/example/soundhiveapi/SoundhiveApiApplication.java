package com.example.soundhiveapi;

import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.model.Tag;
import com.example.soundhiveapi.model.User;
import com.example.soundhiveapi.service.MyJdbcService;
import com.example.soundhiveapi.service.MyJdbcService.TagWeight;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.PriorityQueue;

@SpringBootApplication
public class SoundhiveApiApplication implements CommandLineRunner {

	@Autowired
	private MyJdbcService myJdbcService;

	// Define how many top tags to print.
	private static final int TOP_N = 5;  // Change this value to determine the amount of tag to print from the heap.

	public static void main(String[] args) {
		SpringApplication.run(SoundhiveApiApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// Test 1: Retrieve a user by email.
		String testEmail = "mark.wilson34@gmail.com";  // Adjust this to a real email in your database.
		User user = myJdbcService.getUserByEmail(testEmail);
		if (user != null) {
			System.out.println("User found: " + user.getUsername());
		} else {
			System.out.println("User not found with email: " + testEmail);
			return;
		}

		// Build the user x tag weights matrix.
		double[][] matrix = myJdbcService.getUserTagWeightsMatrix();
		// Retrieve distinct user IDs.
		List<String> distinctUserIds = myJdbcService.getDistinctUserIds();
		// Retrieve all tags (order must match matrix columns).
		List<Tag> allTags = myJdbcService.getAllTags();

		//Retrieve user tag - weight vector
		double[] vec = myJdbcService.getUserTagWeightsArray(user.getIdNumber());
		for (int i = 0; i < allTags.size(); i++) {
			System.out.println(
					allTags.get(i).getTagName()
							+ " (ID:" + allTags.get(i).getTagId() + "): "
							+ vec[i]
			);
		}

		// Retrieve a max-heap (priority queue) of tag weights for the user.
		PriorityQueue<TagWeight> maxHeap = myJdbcService.getUserTagMaxHeap(user.getIdNumber(), matrix, distinctUserIds, allTags);

		// Print the top TOP_N tags by weight.
		System.out.println("Top " + TOP_N + " tags for user " + user.getUsername() + ":");
		for (int i = 0; i < TOP_N && !maxHeap.isEmpty(); i++) {
			System.out.println(maxHeap.poll());
		}
	}
}
