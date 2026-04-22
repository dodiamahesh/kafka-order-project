import { Component, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { OrderService } from '../../services/order.service';
import { Order, OrderStatus, STATUS_CONFIG } from '../../models/order.model';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-list.component.html',
  styleUrls: ['./order-list.component.css']
})
export class OrderListComponent implements OnInit, OnDestroy {

  private orderService = inject(OrderService);

  orders = signal<Order[]>([]);
  filter = signal<OrderStatus | 'ALL'>('ALL');
  highlightedId = signal<number | null>(null);

  statusConfig = STATUS_CONFIG;
  private sub!: Subscription;

  // Track previous order list to detect newly added/updated ones
  private previousIds = new Set<number>();

  ngOnInit(): void {
    // Subscribe to the SSE stream — auto-updates when Kafka consumer changes status
    this.sub = this.orderService.orders$.subscribe(incoming => {
      // Highlight newly arrived orders
      incoming.forEach(o => {
        if (!this.previousIds.has(o.id)) {
          this.highlightedId.set(o.id);
          setTimeout(() => this.highlightedId.set(null), 2000);
        }
      });
      this.previousIds = new Set(incoming.map(o => o.id));
      this.orders.set(incoming);
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  get filteredOrders(): Order[] {
    const f = this.filter();
    if (f === 'ALL') return this.orders();
    return this.orders().filter(o => o.status === f);
  }

  setFilter(f: string): void {
    this.filter.set(f as OrderStatus | 'ALL');
  }

  // Arrow functions are not allowed in Angular templates, so we use a method
  getTabCount(tab: string): number {
    if (tab === 'ALL') return this.orders().length;
    return this.orders().filter(o => o.status === tab).length;
  }

  // Called from template — arrow functions not allowed in Angular interpolation
  // getTabCount(tab: string): number {
  //   if (tab === 'ALL') return this.orders().length;
  //   return this.orders().filter(o => o.status === tab).length;
  // }

  getStatusClass(status: OrderStatus): string {
    return this.statusConfig[status]?.cssClass ?? '';
  }

  getStatusIcon(status: OrderStatus): string {
    return this.statusConfig[status]?.icon ?? '';
  }

  isProcessing(status: OrderStatus): boolean {
    return status === 'PENDING' || status === 'PROCESSING';
  }

  totalAmount(order: Order): number {
    return order.price * order.quantity;
  }

  trackById(_: number, order: Order): number {
    return order.id;
  }
}