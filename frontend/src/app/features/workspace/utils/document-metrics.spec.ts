import {cursorPosition, metrics} from './document-metrics';describe('metrics',()=>it('counts document values',()=>expect(metrics('a b\n').lines).toBe(2)));

describe('cursorPosition', () => { it('reports one-based line and column for the active offset', () => expect(cursorPosition('first\nsecond', 8)).toEqual({ line: 2, column: 3 })); it('clamps offsets to the document', () => expect(cursorPosition('x', 8)).toEqual({ line: 1, column: 2 })); });
