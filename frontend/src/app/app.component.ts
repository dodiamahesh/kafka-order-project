import { Component, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrderFormComponent } from './components/order-form/order-form.component';
import { OrderListComponent } from './components/order-list/order-list.component';
import { OrderService } from './services/order.service';
import { OrderStats } from './models/order.model';
import { interval, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, OrderFormComponent, OrderListComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit, OnDestroy {

  private orderService = inject(OrderService);

  stats = signal<OrderStats>({ total: 0, pending: 0, processing: 0, completed: 0, failed: 0 });
  kafkaConnected = signal(true);

  private statsSub!: Subscription;

  ngOnInit(): void {
    // Open SSE stream — stays connected for real-time order updates
    this.orderService.connectToStream();

    // Poll stats every 3 seconds for the header counters
    this.statsSub = interval(3000).pipe(
      switchMap(() => this.orderService.getStats())
    ).subscribe({
      next: s => this.stats.set(s),
      error: () => this.kafkaConnected.set(false)
    });

    // Load initial stats
    this.orderService.getStats().subscribe({
      next: s => this.stats.set(s),
      error: () => this.kafkaConnected.set(false)
    });
  }

  ngOnDestroy(): void {
    this.orderService.disconnectStream();
    this.statsSub?.unsubscribe();
  }

  onOrderPlaced(): void {
    // Stats will auto-refresh on next interval, but trigger immediately
    this.orderService.getStats().subscribe(s => this.stats.set(s));
  }
}
