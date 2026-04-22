import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, BehaviorSubject } from 'rxjs';
import { Order, OrderRequest, OrderStats } from '../models/order.model';

/**
 * OrderService
 *
 * Handles all communication with the Spring Boot backend:
 *  • HTTP calls via HttpClient (place order, get orders, stats)
 *  • Server-Sent Events (SSE) via native EventSource API
 *    → Real-time push updates when Kafka consumer changes order status
 */
@Injectable({ providedIn: 'root' })
export class OrderService {

  private http = inject(HttpClient);
  private apiUrl = '/api/orders';

  // SSE stream — emits full order list whenever backend pushes an update
  private ordersSubject = new BehaviorSubject<Order[]>([]);
  orders$ = this.ordersSubject.asObservable();

  private eventSource: EventSource | null = null;

  // ── SSE Connection ────────────────────────────────────────

  /**
   * Open a Server-Sent Events connection to Spring Boot.
   *
   * WHY SSE instead of WebSocket or polling?
   * • SSE is one-directional (server → client) — perfect for notifications
   * • No library needed, native browser support (EventSource)
   * • Auto-reconnects on connection drop
   * • HTTP/1.1 compatible (no upgrade needed)
   */
  connectToStream(): void {
    if (this.eventSource) {
      this.eventSource.close();
    }

    this.eventSource = new EventSource(`${this.apiUrl}/stream`);

    // 'orders' matches the event name set in Spring Boot SseEmitter
    this.eventSource.addEventListener('orders', (event: MessageEvent) => {
      const orders: Order[] = JSON.parse(event.data);
      this.ordersSubject.next(orders);
    });

    this.eventSource.onerror = () => {
      console.warn('SSE connection lost — will auto-reconnect in 3s');
      // Browser auto-reconnects EventSource after ~3 seconds
    };

    this.eventSource.onopen = () => {
      console.log('✅ SSE stream connected to Spring Boot');
    };
  }

  disconnectStream(): void {
    this.eventSource?.close();
    this.eventSource = null;
  }

  // ── HTTP API Calls ────────────────────────────────────────

  /**
   * POST /api/orders
   * Returns 201 immediately — Kafka handles the rest async
   */
  placeOrder(request: OrderRequest): Observable<Order> {
    return this.http.post<Order>(this.apiUrl, request);
  }

  /**
   * GET /api/orders
   * Used for initial page load before SSE takes over
   */
  getAllOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(this.apiUrl);
  }

  /**
   * GET /api/orders/stats
   * Dashboard header counters
   */
  getStats(): Observable<OrderStats> {
    return this.http.get<OrderStats>(`${this.apiUrl}/stats`);
  }
}
