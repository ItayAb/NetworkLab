import java.io.FileNotFoundException;
import java.io.IOException;


public class Program {
	
	public static void main(String[] args) {
		try {
			Server server = new Server();
			server.Begin();					
		} catch (ExceptionInInitializerError e) {
			System.out.println(e.getMessage());
		}
		catch (FileNotFoundException e) {
			System.out.println("Config.ini could not be found!");
		}
		catch (IOException e) {
				System.out.println("Error in reading from config.ini");
		} catch (Exception e) {			
			System.out.println("Unexpected error in server initialization!");
		}
		finally
		{			
			System.out.println("Server will shut down");
		}
	}
}
