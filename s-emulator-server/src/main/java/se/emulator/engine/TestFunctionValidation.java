package se.emulator.engine;

public class TestFunctionValidation {
    public static void main(String[] args) {
        EngineService engine = new EngineServiceImpl();
        
        System.out.println("Testing function validation with error-1.xml...");
        
        // Try to load error-1.xml which contains undefined "Plus" function
        LoadResult result = engine.loadSystemFile("03_Tests_Provided\\error-1.xml");
        
        if (result.isSuccess()) {
            System.out.println("ERROR: File loaded successfully - validation failed!");
            System.out.println("This should not happen - Plus function is not defined");
        } else {
            System.out.println("SUCCESS: File rejected as expected");
            System.out.println("Error message: " + result.getMessage());
        }
        
        System.out.println("\nTesting with a valid file...");
        
        // Try to load a valid file for comparison
        LoadResult validResult = engine.loadSystemFile("03_Tests_Provided\\minus.xml");
        
        if (validResult.isSuccess()) {
            System.out.println("SUCCESS: Valid file loaded correctly");
        } else {
            System.out.println("ERROR: Valid file was rejected: " + validResult.getMessage());
        }
    }
}