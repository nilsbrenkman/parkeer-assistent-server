export interface Parking {
    id: number,
    license: string,
    name: string | null,
    startTime: string,
    endTime: string,
    cost: number
}