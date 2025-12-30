export function safeParse<T = any>(s: string) {
  try {
    return JSON.parse(s) as T
  } catch {
    return null as any
  }
}
