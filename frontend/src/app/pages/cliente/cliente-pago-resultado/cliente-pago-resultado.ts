import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { switchMap, take, takeWhile, timer } from 'rxjs';

import { ClienteService } from '../../../services/cliente.service';
import { mensajeError } from '../../../services/mensaje-error';

type EstadoResultado = 'confirmando' | 'aprobado' | 'rechazado' | 'pendiente';

// Página a la que Wompi devuelve al usuario. Volver aquí NO confirma el pago:
// el que lo confirma es el webhook del backend. Por eso se consulta la reserva cada 2 segundos
// hasta que el pago deja de estar PENDIENTE (máximo 15 intentos).
@Component({
  selector: 'app-cliente-pago-resultado',
  imports: [RouterLink],
  templateUrl: './cliente-pago-resultado.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClientePagoResultado implements OnInit {
  private clienteService = inject(ClienteService);
  private destroyRef = inject(DestroyRef);

  reservaId = input.required<string>();

  protected readonly estado = signal<EstadoResultado>('confirmando');
  protected readonly error = signal('');

  ngOnInit(): void {
    timer(0, 2000)
      .pipe(
        switchMap(() => this.clienteService.reserva(+this.reservaId())),
        takeWhile((item) => item.pago?.estado !== 'COMPLETADO' && item.pago?.estado !== 'RECHAZADO', true),
        take(15),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (item) => {
          if (item.pago?.estado === 'COMPLETADO') {
            this.estado.set('aprobado');
          } else if (item.pago?.estado === 'RECHAZADO') {
            this.estado.set('rechazado');
          }
        },
        error: (err: HttpErrorResponse) => this.error.set(mensajeError(err)),
        complete: () => {
          if (this.estado() === 'confirmando') {
            this.estado.set('pendiente');
          }
        },
      });
  }
}
