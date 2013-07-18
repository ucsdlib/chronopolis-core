/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.OpenOption;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 * @author shake
 */
public class BagValidator {
	
	public final String bagInfo = "bag-info.txt";
	public static final String manifest = "manifest-*.txt";
	public static final String charset = "UTF-8";
	public final String bagit = "bagit.txt";
	private final ExecutorService manifestRunner = Executors.newCachedThreadPool();
	private final Path toBag;
	private Future<Boolean> f;
	
	
	public BagValidator(Path toBag) {
		this.toBag = toBag;
		f = validateManifest();
	}
	
	public static boolean ValidateBagFormat(Path toBag) {
		return true;
	}
	
	public Future<Boolean> getFuture() {
		return f;
	}

	private String byteToHex(byte[] bytes) {
		StringBuilder str = new StringBuilder(new BigInteger(1, bytes).toString(16));
		if ( str.length() < bytes.length*2) {
			for ( int i=0; i < bytes.length*2-str.length(); i++) {
				str.insert(0, "0");
			}
		}
		return str.toString();
	}
	
	private Future<Boolean> validateManifest() {
		return manifestRunner.submit(new Callable<Boolean>(){
			@Override
			public Boolean call() throws Exception {
				HashSet<Path> manifests = new HashSet<>();
				HashMap<Path, String> toCheck = new HashMap<>();
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				boolean valid = true;
				try (DirectoryStream<Path> directory = Files.newDirectoryStream(toBag, manifest)) {
					for ( Path p : directory) {
						System.out.println(p.getFileName());
						System.out.println(p.toAbsolutePath().toString());
						System.out.println(p.toString());
						manifests.add(p);
					}
				}

				for ( Path toManifest : manifests) {
					String digestType = toManifest.getFileName().toString().split("-")[1];
					// There's still the .txt on the end so just match
					if ( digestType.matches("sha256") ) {
						md = MessageDigest.getInstance("SHA-256");
					}

					try (BufferedReader reader = Files.newBufferedReader(toManifest, Charset.forName("UTF-8"))) {
						String[] line = reader.readLine().split("\\s+", 2);
						String digest = line[0];
						String file = line[1];
						toCheck.put(Paths.get(toBag.toString(), file), digest);
					}
				}	

				for ( Map.Entry<Path, String> entry : toCheck.entrySet()) {
					Path toFile = entry.getKey();
					String registeredDigest = entry.getValue();

					FileInputStream fis = new FileInputStream(toFile.toFile());
					DigestInputStream dis = new DigestInputStream(fis, md);
					dis.on(true);
					int off = 0;
					int toread = (1024 <= dis.available() ? 1024 : dis.available());
					byte []buf = new byte[dis.available()];
					while ( dis.read(buf, off, toread) > 0) {
						toread = (1024 <= dis.available() ? 1024 : dis.available());
					}
					byte[] calculatedDigest = md.digest();
					String digest = byteToHex(calculatedDigest);
					valid = registeredDigest.equals(digest);
				}

				return valid;
			}

		});
	}
	
	
	private class ManifestValidator implements Runnable {
		
		@Override
		public void run() {
			System.out.println("IMMAJEP");
		}
		
	}
	
	public void ValidateManifestAndGetTokens() throws IOException {
		//Filter f = new Filter();
		HashSet<Path> manifests = new HashSet<>();
		try (DirectoryStream<Path> directory = Files.newDirectoryStream(toBag, manifest)) {
			for ( Path p : directory) {
				manifests.add(p);
			}
		}
		
		for ( Path toManifest : manifests ) {
			BufferedReader br = Files.newBufferedReader(toBag, Charset.forName(charset));
			String line = null;
			while ( (line = br.readLine()) != null) {
				String[] split = line.split(" ");
				String digest = split[0];
				String file = split[1];
			}
		}
		
	}
	
}
