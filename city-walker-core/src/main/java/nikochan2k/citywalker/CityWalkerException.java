package nikochan2k.citywalker;

public class CityWalkerException extends Exception {

	public CityWalkerException(String message) {
		super(message);
	}
	
	public CityWalkerException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public CityWalkerException(Throwable cause) {
		super(cause);
	}
	
}
