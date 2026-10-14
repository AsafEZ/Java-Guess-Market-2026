import assert from 'node:assert/strict';
import { test } from 'node:test';
import { depositAmount, formatTime, selectedPosition } from './accountModel.js';

test('deposit entry accepts positive finite amounts only', () => {
  assert.equal(depositAmount('10.5'), 10.5);
  assert.equal(depositAmount(''), null);
  assert.equal(depositAmount('0'), null);
  assert.equal(depositAmount('-2'), null);
  assert.equal(depositAmount('Infinity'), null);
  assert.equal(depositAmount('abc'), null);
});

test('position selection uses a stable event ID across refreshed DTOs', () => {
  const first = [{ eventId: 4, eventName: 'A' }];
  const refreshed = [{ eventId: 4, eventName: 'A', totalShares: 3 }];
  assert.equal(selectedPosition(first, 4), first[0]);
  assert.equal(selectedPosition(refreshed, 4), refreshed[0]);
  assert.equal(selectedPosition(refreshed, 5), null);
});

test('history timestamps are readable and invalid values remain visible', () => {
  assert.match(formatTime('2026-10-14T16:05:11.125415300Z'), /14\/10\/2026/);
  assert.equal(formatTime(null), '-');
  assert.equal(formatTime('unrecognized'), 'unrecognized');
});
