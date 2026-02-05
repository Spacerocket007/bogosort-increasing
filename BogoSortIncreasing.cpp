#include <iostream>
#include <random>
#include <vector>
#include <chrono>
#include <thread>
#include <atomic>
#include <mutex>
#include <string>

using namespace std;

bool printEnabled = false;
bool running = true;

class BogoSortIncreasing {
public:
    BogoSortIncreasing() : eng(random_device{}()) {}

    bool isSorted(const vector<int> &arr) const {
        for (size_t i = 0; i + 1 < arr.size(); ++i)
            if (arr[i] > arr[i + 1]) return false;
        return true;
    }

    // Helper for printing
    static void printArray(const vector<int> &arr) {
        for (size_t i = 0; i < arr.size(); ++i) {
            cout << arr[i] << (i + 1 < arr.size() ? " " : "");
        }
        cout << endl;
    }

private:
    mt19937 eng;
};

int main() {
    BogoSortIncreasing sorter;
    string cmd;
    int arrSize = 2; // Start with 2 elements

    cout << "Type in: shuffle to start \n";


    while (running) {
        if (!getline(cin, cmd)) break;
        if (cmd == "exit" || cmd == "quit") {
            running = false;
            break;
        }
        if (cmd == "shuffle") {
            while (running) {
                vector<int> base(arrSize);
                for (int i = 0; i < arrSize; ++i) base[i] = i + 1;

                size_t numThreads = std::thread::hardware_concurrency();
                if (numThreads == 0) numThreads = 1;

                atomic<bool> found(false);
                atomic<long long> totalAttempts(0);
                vector<int> winner;
                mutex winnerMutex;
                vector<std::thread> workers;

                auto start = chrono::steady_clock::now();

                for (size_t k = 0; k < numThreads; ++k) {
                    workers.emplace_back([&, k]() {
                        std::mt19937 eng_local(std::random_device{}() + static_cast<unsigned>(k));
                        std::vector<int> local = base;
                        long long localCount = 0;

                        // Initial shuffle so it doesn't start already sorted
                        for (int i = static_cast<int>(local.size()) - 1; i > 0; --i) {
                            std::swap(local[i], local[std::uniform_int_distribution<int>(0, i)(eng_local)]);
                        }

                        while (!found && running) {
                            // Fisher-Yates shuffle
                            for (int i = static_cast<int>(local.size()) - 1; i > 0; --i) {
                                std::uniform_int_distribution<int> dist(0, i);
                                std::swap(local[i], local[dist(eng_local)]);
                            }

                            localCount++;

                            if (sorter.isSorted(local)) {
                                bool expected = false;
                                if (found.compare_exchange_strong(expected, true)) {
                                    std::lock_guard<std::mutex> lk(winnerMutex);
                                    winner = local;
                                }
                                break;
                            }

                            // Performance optimization: update atomic every 1000 shuffles
                            if (localCount >= 1000) {
                                totalAttempts.fetch_add(localCount, std::memory_order_relaxed);
                                localCount = 0;
                            }
                        }
                        totalAttempts.fetch_add(localCount, std::memory_order_relaxed);
                    });
                }

                for (auto &th : workers) {
                    if (th.joinable()) th.join();
                }

                auto end = chrono::steady_clock::now();
                double elapsedMs = chrono::duration<double, milli>(end - start).count();

                if (found) {
                    cout << "\n--- Size " << arrSize << " Completed ---" << endl;
                    if (printEnabled) {
                        cout << "Result: ";
                        BogoSortIncreasing::printArray(winner);
                    }
                    cout << "Total shuffles: " << totalAttempts.load() << endl;
                    cout << "Time: " << elapsedMs << " ms" << endl;


                    arrSize++;
                    cout << "Next challenge size: " << arrSize << "\n" << endl;
                }
            }

        } else {
            cout << "Unknown command.\n";
        }
    }
    return 0;
}