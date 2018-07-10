import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.vmichalak.sonoscontroller.SonosDevice;
import com.vmichalak.sonoscontroller.SonosDiscovery;
import com.vmichalak.sonoscontroller.exception.SonosControllerException;
import com.vmichalak.sonoscontroller.model.TrackInfo;
import com.vmichalak.sonoscontroller.model.TrackMetadata;

public class Main {
	private static final int END_SILENCER_MINS = 30;
	private static final int END_SILENCER_HOURS = 18;
	private static final int MAX_VOLUME_FOR_JULIANS_EARS = 18;
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static void log(String messageFormat, Object... objects) {
		Date currentDateTime = new Date(System.currentTimeMillis());
		System.out.println(
				String.format(
						String.format("%s [ INFO ] ",  SIMPLE_DATE_FORMAT.format(currentDateTime)) + messageFormat,
						objects));
	}

	public static void main(String[] args) throws IOException, SonosControllerException {
		boolean ok = true;
		boolean hasSomeoneChangedTheVolume = false;

		while(ok) {
			List<SonosDevice> devices = SonosDiscovery.discover();
			Date currentDateTime = new Date(System.currentTimeMillis());

			// if it is past 7pm - I will not bother - let the people have fun

			Calendar calender = Calendar.getInstance();
			int hourOfDay = calender.get(Calendar.HOUR_OF_DAY);
			int minutesOfDay = calender.get(Calendar.MINUTE);

			if(hourOfDay >= END_SILENCER_HOURS && minutesOfDay >= END_SILENCER_MINS) {
				log("Time's up - let the people have fun after hour %d:%d", hourOfDay, minutesOfDay);
				return;
			}

			for (SonosDevice sonosDevice : devices) {
				String zoneName = sonosDevice.getZoneName();

				if(!zoneName.equals("")) {
					TrackInfo currentTrackInfo = sonosDevice.getCurrentTrackInfo();
					TrackMetadata metadata = currentTrackInfo.getMetadata();

					String title = metadata.getTitle();
					String creator = metadata.getCreator();
					if(null != title && title.length() != 0) {
						log("Now playing '%s', by '%s'", title, creator);
					}

					try {
						int currentVolume = sonosDevice.getVolume();

						if(currentVolume > MAX_VOLUME_FOR_JULIANS_EARS) {
							int newVolume = currentVolume - 1;
							sonosDevice.setVolume(newVolume);

							hasSomeoneChangedTheVolume = true;
							log("Naughty, naughty - subtly reducing the volume from %d to %d - maximum volume is %d for zone name '%s'",
									currentVolume, 
									newVolume, 
									MAX_VOLUME_FOR_JULIANS_EARS, 
									zoneName);
						}
					} catch(NumberFormatException ex) {
						// ignore - stupid parsing code in another person's library
					}
				}
			}

			try {
				// we are going to randomise this, to make it less explicit
				int numSeconds = new Random(System.currentTimeMillis()).nextInt(13) + 5;

				if(!hasSomeoneChangedTheVolume) {
					numSeconds = numSeconds *2;
				}

//				hasSomeoneChangedTheVolume = false;

				if(hasSomeoneChangedTheVolume) {
					log("Someone changed the volume, sleeping for %d seconds", numSeconds);
				} else {
					log("Nothing happened - Sleeping for %d seconds", numSeconds);
				}

				Thread.sleep(numSeconds * 1000);
			} catch (InterruptedException e) {
				log("Oh dear, I was interrupted - probably enough fun for today...");
				ok = false;
			}

			if(hasSomeoneChangedTheVolume) {
				hasSomeoneChangedTheVolume = false;
			}
		}
	}
}
