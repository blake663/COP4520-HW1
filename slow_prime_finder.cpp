#include <bits/stdc++.h>

using namespace std;

const long long n = 1e8;
long long countt = 0;
vector<char> sieve;


int main() {
    sieve = vector<char>(n+1, true);

    cout << "starting... " << endl;

    long long count = 0;
    for (int x = 2; x <= n; x++) {
        if (!sieve[x]) continue;

        count++;
        for (int c = 2*x; c <= n; c += x) {
            sieve[c] = false;
        }
    }
    cout << count << endl;
}
