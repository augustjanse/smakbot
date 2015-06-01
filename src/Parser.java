import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import javax.mail.*;
import javax.mail.internet.*;
import javax.script.ScriptException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;

/**
 * Examines if Smaktestarna has any products available right now and attempts to
 * order them.
 * 
 * @author August Janse
 * @version 2014-05-03
 * 
 */
public class Parser {
	boolean mailNotification = true;
	boolean consoleNotification = true;

	HashMap<Integer, Element> availableArticles;
	TreeMap<Integer, Integer> prefs;

	/**
	 * Notifies the user of availability of products.
	 * 
	 * @param args
	 */
	public Parser() {
		try {
			availableArticles = findAvailableArticles();
			if (availableArticles.isEmpty()) {
				System.out.println("Inga varor fanns.");
				System.exit(0);
			}
		} catch (IOException e1) {
			System.err.println("Couldn't parse website, terminating.");
			e1.printStackTrace();
			System.exit(1);
		}

		try {
			prefs = readPreferences();
		} catch (IOException e1) {
			System.err
					.println("Error reading preferences document. Terminating.");
			e1.printStackTrace();

			System.exit(2);
		}

		try {
			int preferredIndex = 0;
			while (preferredIndex == 0) {
				Entry<Integer, Integer> entry = prefs.pollFirstEntry();
				int key = -1;
				if (entry != null) {
					key = entry.getValue();
				} else {
					System.out
							.println("Vissa varor fanns, men inga önskade. Välj nya varor i GUI:t om du har ändrat dig.");
					System.exit(0);
				}

				if (availableArticles.containsKey(key)) {
					preferredIndex = key;
				}
			}

			orderArticle(preferredIndex);
		} catch (ScriptException e) {
			e.printStackTrace();
			System.err
					.println("Couldn't order automatically."); 	// TODO: Attempt to send notices instead.
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new Parser();
	}

	/**
	 * Prints a message to the console. Not used.
	 * 
	 * @args product Name of the available product
	 * @args available true if available, false otherwise
	 */
	private void notifyByConsole(String product, boolean available) {
		if (available) {
			System.out.println("Nu finns det " + product + "!");
		} else {
			System.out.println("Varan fanns inte.");
		}
	}

	/**
	 * Mails a user the name of an available product, and a link to
	 * the site. Not used.
	 * 
	 * @args product Name of the available product
	 */
	private void notifyByMail(String product) {
		final String username = "user@gmail.com"; // should probably store these in a config file instead
		final String password = "password";

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		Session session = Session.getInstance(props,
				new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(username, password);
					}
				});

		try {
			Message message = new MimeMessage(session);

			message.setFrom(new InternetAddress(username, "Smakbot"));

			message.addRecipient(Message.RecipientType.TO, new InternetAddress(
					"example@example.com", "Mr. Example"));

			message.setSubject("Nu finns Smaktestarnavaror!");
			message.setText("Nu finns det "
					+ product
					+ "! Klicka här för att få en kupong:\n\nhttps://www.coop.se/Butiker-varor--erbjudanden/Vara-varor--varumarken1/Smaktestarna/");

			Transport.send(message);
		} catch (MessagingException | UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Orders article with index n (from 0). Not finished.
	 * 
	 * @throws ScriptException
	 * @throws FileNotFoundException
	 */
	private void orderArticle(int n) throws ScriptException,
			FileNotFoundException {
		String profile = "default-1403984282828";

		ProfilesIni profileObj = new ProfilesIni();
		FirefoxProfile firefoxProfile = profileObj.getProfile(profile);
		WebDriver driver = new FirefoxDriver(firefoxProfile);
		driver.get("https://www.coop.se/Butiker-varor--erbjudanden/Vara-varor--varumarken1/Smaktestarna/");

		// För produktnumret måste rätt knapp hittas. Det är det som är det
		// svåra. Hoppas att alla knappar har samma ID olika gånger.
		// Detta måste undersökas närmare när tillfälle ges.
		driver.findElements(By.cssSelector("article")).get(n)
				.findElement(By.name("selectedCouponGuid")).click();

		// Fields are already filled, hopefully.

		/*
		 * driver.findElement(
		 * By.id("ea85dc3d-9c0f-438b-8de4-a992ad466212-form-name")) .clear();
		 * driver.findElement( By.)) .sendKeys(name); driver.findElement(
		 * By.id("ea85dc3d-9c0f-438b-8de4-a992ad466212-form-tel")).clear();
		 * driver.findElement(
		 * By.id("ea85dc3d-9c0f-438b-8de4-a992ad466212-form-tel"))
		 * .sendKeys(phone); driver.findElement(
		 * By.id("ea85dc3d-9c0f-438b-8de4-a992ad466212-form-email")) .clear();
		 * driver.findElement(
		 * By.id("ea85dc3d-9c0f-438b-8de4-a992ad466212-form-email"))
		 * .sendKeys(mail);
		 */

		driver.findElement(By.name("productCouponForm")).click();

	}

	/**
	 * Returns all available articles mapped to their indexes, or null.
	 * 
	 * @return
	 * @throws IOException
	 */
	private HashMap<Integer, Element> findAvailableArticles()
			throws IOException {
		HashMap<Integer, Element> articleMap = new HashMap<>();

		Document doc = Jsoup
				.connect(
						"https://www.coop.se/Butiker-varor--erbjudanden/Vara-varor--varumarken1/Smaktestarna/")
				.get();
		Elements articles = doc.select("article");

		int i = 0;
		for (Element article : articles) { // First 8
			String text = article.text();

			if (text.contains("Den tar jag!")) {
				articleMap.put(i, article);
			} else if (text
					.contains("Just nu finns ingen vara. Prova igen senare.")) {
				// Do nothing
			} else {
				throw new IllegalArgumentException(
						"Found neither of the expected strings.");
			}

			i++;
		}

		return articleMap;
	}

	/**
	 * Returns a map of indexes sorted in preferred (not numeric) order.
	 * 
	 * @return
	 * @throws IOException
	 */
	private TreeMap<Integer, Integer> readPreferences() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(
				"pref.txt")));
		int[] prefs = new int[8];
		TreeMap<Integer, Integer> iMap = new TreeMap<Integer, Integer>();

		int i = 0;
		String line = reader.readLine();
		while (line != null) {
			if (line.matches("\\d+")) {
				prefs[i] = Integer.parseInt(line);
			}

			line = reader.readLine();
			i++;
		}

		for (int j = 0; j < prefs.length; j++) {
			if (prefs[j] > 0) {
				iMap.put(prefs[j], j);
			}
		}

		reader.close();
		return iMap;
	}
}
