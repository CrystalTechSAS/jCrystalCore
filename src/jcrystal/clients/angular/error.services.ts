/**
 * Created by AndreaC on 13/02/2017.
 */
import { Injectable } from '@angular/core';
import { Subject }    from 'rxjs';

@Injectable()
export class ErrorService {
  // Observable string sources
  private missionAnnouncedSource = new Subject<string>();
  private missionConfirmedSource = new Subject<string>();
  // Observable string streams
  missionAnnounced$ = this.missionAnnouncedSource.asObservable();
  missionConfirmed$ = this.missionConfirmedSource.asObservable();
  // Service message commands
  announceError(mission: string) {
    this.missionAnnouncedSource.next(mission);
  }

}
