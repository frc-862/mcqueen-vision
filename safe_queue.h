#pragma once 

#include <queue>
#include <mutex>
#include <condition_variable>

// threadsafe queue
template <class T>
class SafeQueue
{
public:
  SafeQueue() : q(), m(), c() {}
  ~SafeQueue() {}

  // Add an element to the queue.
  void push(T t) {
    std::lock_guard<std::mutex> lock(m);
    q.push(t);
    c.notify_one();
  }

  // Take an element from the queue
  T shift()
  {
    std::unique_lock<std::mutex> lock(m);
    c.wait(lock, [&] { return !q.empty(); });
    T val = q.front();
    q.pop();
    return val;
  }

private:
  mutable std::mutex m;
  std::condition_variable c;
  std::queue<T> q;
};

