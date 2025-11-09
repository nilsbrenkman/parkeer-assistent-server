import type {Regime} from './Regime.tsx';

export interface User {
    balance: string;
    hourRate: number;
    regimeTimeStart: string | null;
    regimeTimeEnd: string | null;
    regime: Regime;
}