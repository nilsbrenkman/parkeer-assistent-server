type RequestMethod = 'GET' | 'POST' | 'DELETE';

interface FetchOptions {
    method?: RequestMethod;
    body?: unknown;
}

interface ErrorMessage {
    message: string;
    details?: string;
}

export interface ErrorResponse {
    status: number;
    message: string;
    details?: string;
}

interface ApiResponse<T> {
    data?: T;
    error?: ErrorResponse;
    noContent?: boolean,
}

export async function fetchWrapper<T>(
    url: string,
    options: FetchOptions = {},
): Promise<ApiResponse<T>> {

    const method: RequestMethod = options.method ?? 'GET';
    const headers: HeadersInit | undefined = (options.body)
        ? {
            'Content-Type': 'application/json',
        }
        : undefined;
    const body: BodyInit = (options.body && typeof options.body === 'object')
        ? JSON.stringify(options.body)
        : options.body as BodyInit;

    try {
        const response = await fetch(url, {
            method,
            headers,
            body,
            credentials: 'include',
        });

        if (response.status === 401) {
            window.location.href = '/';
            return {error: {message: 'Unauthorized', status: 401}};
        }

        if (!response.ok) {
            const error = await response.json().catch(() => null) as ErrorMessage | null;
            return {
                error: {
                    status: response.status,
                    message: error?.message ?? 'Unknown error',
                    details: error?.details,
                },
            };
        }

        const data = await response.json().catch(() => null) as T | undefined;

        if (data === null && (response.status === 201 || response.status === 202 || response.status === 204)) {
            return {noContent: true};
        }
        return {data};
    } catch (error) {
        console.error('API Error:', error);
        return {
            error: {
                message: 'Network error',
                status: 0,
            },
        };
    }
}
