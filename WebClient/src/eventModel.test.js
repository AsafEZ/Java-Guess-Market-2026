import assert from 'node:assert/strict';
import { test } from 'node:test';
import { availableActions, filterEvents, positivePrice, positiveWhole, sameData } from './eventModel.js';

const sample = [
  { name: 'Rain tomorrow', tradingMethod: 'LMSR', status: 'ACTIVE', commissionType: 'ON_PURCHASE' },
  { name: 'Election result', tradingMethod: 'ORDER_BOOK', status: 'NOT_STARTED', commissionType: 'ON_CLOSE' },
];

test('event filters combine without changing the original list', () => {
  assert.deepEqual(filterEvents(sample, { search: ' RAIN ', method: 'LMSR', status: 'ACTIVE', commission: '' }), [sample[0]]);
  assert.equal(sample.length, 2);
});

test('only the market maker can open or close an event', () => {
  assert.deepEqual(availableActions({ status: 'NOT_STARTED', marketMakerName: 'A' }, 'B'),
    { open: false, purchase: false, order: false, close: false });
  assert.equal(availableActions({ status: 'NOT_STARTED', marketMakerName: 'A' }, 'A').open, true);
  assert.equal(availableActions({ status: 'ACTIVE', tradingMethod: 'LMSR', marketMakerName: 'A' }, 'B').purchase, true);
  assert.equal(availableActions({ status: 'ACTIVE', tradingMethod: 'ORDER_BOOK', marketMakerName: 'A' }, 'A').close, true);
});

test('numeric forms reject fractions, zero, overflow, and nonfinite prices', () => {
  assert.equal(positiveWhole('3'), 3);
  assert.equal(positiveWhole('0'), null);
  assert.equal(positiveWhole('1.5'), null);
  assert.equal(positiveWhole('99999999999999999999'), null);
  assert.equal(positivePrice('0.75'), 0.75);
  assert.equal(positivePrice('Infinity'), null);
  assert.equal(positivePrice('-1'), null);
});

test('unchanged DTO snapshots do not need a UI replacement', () => {
  assert.equal(sameData(sample, structuredClone(sample)), true);
  assert.equal(sameData(sample, [...sample, { name: 'New' }]), false);
});
