package Service;

public class ComputerService {
    public static int getProcessorCount() {
        return Runtime.getRuntime().availableProcessors();
    }
}
