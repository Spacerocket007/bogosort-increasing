import java.util.Random;
import java.util.Scanner;
import javax.sound.sampled.*;

public class BogoSortIncreasing {
    private static final Random random = new Random();

    private static volatile boolean printEnabled = false;
    private static volatile boolean running = true;

    private static boolean isSorted(int[] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i] > arr[i + 1]) {
                return false;
            }
        }
        return true;
    }

	private static void shuffle(int[] arr) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;
        }
    }

    private static long bogosort(int[] arr) {
        long attempts = 0;

        while (running && !isSorted(arr)) {
            if (printEnabled) {
                printArray(arr);
            }
            shuffle(arr);
            attempts++;
        }

        if (printEnabled) {
            printArray(arr);
        }

        return attempts;
    }

    private static void printArray(int[] arr) {
        StringBuilder sb = new StringBuilder("[ ");
        for (int v : arr) {
            sb.append(v).append(" ");
        }
        sb.append("]");
        System.out.println(sb);
    }


    private static void beep() {
	byte[] buf = new byte[1];
        AudioFormat af = new AudioFormat(8000f, 8, 1, true, false);
        try (SourceDataLine sdl = AudioSystem.getSourceDataLine(af)) {
            sdl.open(af);
            sdl.start();
            for (int i = 0; i < 8000 / 10; i++) {
                double angle = i / (8000f / 440) * 2.0 * Math.PI;
                buf[0] = (byte) (Math.sin(angle) * 100);
                sdl.write(buf, 0, 1);
            }
            sdl.drain();
        } catch(Exception e) {
	    System.out.println("Error: " + e.getMessage());
	}
    }

    private static void init(String[] args) {
        if (args.length == 0) {
            return;
        } else if (args.length > 1) {
            throw new IllegalArgumentException("Invalid number of Arguments");
        }

        switch (args[0]) {
            case "on" -> printEnabled = true;
            case "off" -> printEnabled = false;
            default -> throw new IllegalArgumentException("Invalid Command passed");
        }
    }

    public static void main(String[] args) {

        // Command input thread
        Thread inputThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (running) {
                String cmd = scanner.nextLine().trim().toLowerCase();
                switch (cmd) {
                    case "on":
                        printEnabled = true;
                        System.out.println("Printing enabled");
                        break;
                    case "off":
                        printEnabled = false;
                        System.out.println("Printing disabled");
                        break;
                    case "toggle":
                        printEnabled = !printEnabled;
                        System.out.println("Printing " + (printEnabled ? "enabled" : "disabled"));
                        break;
                    case "quit":
                        running = false;
                        System.out.println("Stopping...");
                        break;
                    default:
                        System.out.println("Commands: on | off | toggle | quit");
                }
            }
            scanner.close();
        });

        // Sorting thread
        Thread sortingThread = new Thread(() -> {
            int size = 2;

            while (running) {
                int[] arr = new int[size];

                for (int i = 0; i < size; i++) {
                    arr[i] = i;
                }

                shuffle(arr);

                System.out.println("\nSorting array of size " + size + "...");

                long startTime = System.nanoTime();
                long attempts = bogosort(arr);
                long endTime = System.nanoTime();

                double elapsedMs = (endTime - startTime) / 1_000_000.0;
                double avgTimePerShuffle = attempts > 0 ? elapsedMs / attempts : 0;

		        beep();
		        System.out.println("Sorted!");
                System.out.println("Shuffles: " + attempts);
                System.out.printf("Total time: %.3f ms%n", elapsedMs);
                System.out.printf("Avg time per shuffle: %.6f ms%n", avgTimePerShuffle);

                size++;
            }
        });

        init(args);
        inputThread.start();
        sortingThread.start();
    }
}

