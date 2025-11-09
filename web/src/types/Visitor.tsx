export interface Visitor {
    visitorId: number;
    permitId: number;
    license: string;
    formattedLicense: string;
    name: string | undefined;
}