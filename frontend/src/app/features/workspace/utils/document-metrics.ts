export interface DocumentMetrics { words:number; characters:number; lines:number; bytes:number }
export function metrics(content:string):DocumentMetrics{return {words:(content.trim().match(/\S+/g)??[]).length,characters:content.length,lines:content.split('\n').length,bytes:new TextEncoder().encode(content).length};}

export interface CursorPosition { line: number; column: number }
export function cursorPosition(content: string, offset: number): CursorPosition { const before = content.slice(0, Math.max(0, Math.min(offset, content.length))); const lines = before.split('\n'); return { line: lines.length, column: (lines.at(-1)?.length ?? 0) + 1 }; }
