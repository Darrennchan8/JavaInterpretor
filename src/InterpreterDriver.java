import java.util.Scanner;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

public class InterpreterDriver {
	public static void main(String[] arguments) {
		Interpreter interpreter = new Interpreter();
		if (arguments.length == 0) {
			Scanner scan = new Scanner(System.in);
			while (true) {
				System.out.print(">> ");
				String result = interpreter.interpret(scan.nextLine()).toString();
				if (result.equals("\"exit\"")) {
					break;
				}
				System.out.println(result);
			}
		} else if (arguments.length == 1) {
			try {
				File fileObj = new File(arguments[0]);
				Path file = fileObj.toPath();
				byte[] fileArray = Files.readAllBytes(file);
				String result = interpreter.interpret(new String(fileArray)).toString();
				System.out.println(result);
			} catch (IOException err) {
				err.printStackTrace();
			}
		} else {
			throw new Error("Invalid Arguments");
		}
	}
}