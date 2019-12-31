import { QuestEventDagModel } from "./QuestEventDagModel";
import { QuestEventModel } from "./QuestEventModel";
import { WaveModel } from "./WaveModel";

test("Adding a root event should result in a single connected sub graph.", () => {
    const dag = new QuestEventDagModel(1);

    expect(dag.connected_sub_graphs.length.val).toBe(0);

    dag.add_event(new QuestEventModel(1, 1, new WaveModel(1, 1, 1), 30, 0), [], []);

    expect(dag.connected_sub_graphs.length.val).toBe(1);
    expect(dag.connected_sub_graphs.get(0).length.val).toBe(1);
});

test("Adding a disconnected event should result in a correct graph.", () => {
    const dag = new QuestEventDagModel(1);
    const event1 = new QuestEventModel(1, 1, new WaveModel(1, 1, 1), 30, 0);
    const event2 = new QuestEventModel(2, 1, new WaveModel(2, 1, 1), 30, 0);

    dag.add_event(event1, [], []);
    dag.add_event(event2, [], []);

    expect(dag.get_children(event1)).toEqual([]);
    expect(dag.get_parents(event1)).toEqual([]);

    expect(dag.get_children(event2)).toEqual([]);
    expect(dag.get_parents(event2)).toEqual([]);

    expect(dag.connected_sub_graphs.length.val).toBe(2);
    expect(dag.connected_sub_graphs.get(0).val).toEqual([event1]);
    expect(dag.connected_sub_graphs.get(1).val).toEqual([event2]);
});

test("Adding a child event should result in a correct graph.", () => {
    const dag = new QuestEventDagModel(1);
    const event1 = new QuestEventModel(1, 1, new WaveModel(1, 1, 1), 30, 0);
    const event2 = new QuestEventModel(2, 1, new WaveModel(2, 1, 1), 30, 0);

    dag.add_event(event1, [], []);
    dag.add_event(event2, [event1], []);

    expect(dag.get_children(event1)).toEqual([event2]);
    expect(dag.get_parents(event1)).toEqual([]);

    expect(dag.get_children(event2)).toEqual([]);
    expect(dag.get_parents(event2)).toEqual([event1]);

    expect(dag.connected_sub_graphs.length.val).toBe(1);
    expect(dag.connected_sub_graphs.get(0).val).toEqual([event1, event2]);
});

test("Adding a parent event should result in a correct graph.", () => {
    const dag = new QuestEventDagModel(1);
    const event1 = new QuestEventModel(1, 1, new WaveModel(1, 1, 1), 30, 0);
    const event2 = new QuestEventModel(2, 1, new WaveModel(2, 1, 1), 30, 0);

    dag.add_event(event1, [], []);
    dag.add_event(event2, [], [event1]);

    expect(dag.get_children(event1)).toEqual([]);
    expect(dag.get_parents(event1)).toEqual([event2]);

    expect(dag.get_children(event2)).toEqual([event1]);
    expect(dag.get_parents(event2)).toEqual([]);

    expect(dag.connected_sub_graphs.length.val).toBe(1);
    expect(dag.connected_sub_graphs.get(0).val).toEqual([event1, event2]);
});

test("Adding an event with a connecting edge should join two sub graphs.", () => {
    const dag = new QuestEventDagModel(1);
    const event1 = new QuestEventModel(1, 1, new WaveModel(1, 1, 1), 30, 0);
    const event2 = new QuestEventModel(2, 1, new WaveModel(2, 1, 1), 30, 0);
    const event3 = new QuestEventModel(3, 1, new WaveModel(3, 1, 1), 30, 0);

    dag.add_event(event1, [], []);
    dag.add_event(event2, [], []);
    dag.add_event(event3, [event2], [event1]);

    expect(dag.get_children(event1)).toEqual([]);
    expect(dag.get_parents(event1)).toEqual([event3]);

    expect(dag.get_children(event2)).toEqual([event3]);
    expect(dag.get_parents(event2)).toEqual([]);

    expect(dag.get_children(event3)).toEqual([event1]);
    expect(dag.get_parents(event3)).toEqual([event2]);

    expect(dag.connected_sub_graphs.length.val).toBe(1);
    expect(dag.connected_sub_graphs.get(0).val).toEqual([event1, event2, event3]);
});

test("Inserting an event should result in a correct graph.", () => {
    const dag = new QuestEventDagModel(1);
    const event1 = new QuestEventModel(1, 1, new WaveModel(1, 1, 1), 30, 0);
    const event2 = new QuestEventModel(2, 1, new WaveModel(2, 1, 1), 30, 0);
    const event3 = new QuestEventModel(3, 1, new WaveModel(3, 1, 1), 30, 0);

    dag.add_event(event1, [], []);
    dag.add_event(event2, [], []);
    dag.insert_event(1, event3, [event2], [event1]);

    expect(dag.get_children(event1)).toEqual([]);
    expect(dag.get_parents(event1)).toEqual([event3]);

    expect(dag.get_children(event2)).toEqual([event3]);
    expect(dag.get_parents(event2)).toEqual([]);

    expect(dag.get_children(event3)).toEqual([event1]);
    expect(dag.get_parents(event3)).toEqual([event2]);

    expect(dag.connected_sub_graphs.length.val).toBe(1);
    expect(dag.connected_sub_graphs.get(0).val).toEqual([event1, event3, event2]);
});

test("Removing an event should result in a correct graph.", () => {
    const dag = new QuestEventDagModel(1);
    const event1 = new QuestEventModel(1, 1, new WaveModel(1, 1, 1), 30, 0);
    const event2 = new QuestEventModel(2, 1, new WaveModel(2, 1, 1), 30, 0);

    dag.add_event(event1, [], []);
    dag.add_event(event2, [event1], []);

    // Graph is fully connected at this point.
    expect(dag.connected_sub_graphs.length.val).toBe(1);

    dag.remove_event(event2);

    expect(dag.get_children(event1)).toEqual([]);
    expect(dag.get_parents(event1)).toEqual([]);

    // Event 2 is no longer part of the DAG.
    expect(() => dag.get_children(event2)).toThrowError();
    expect(() => dag.get_parents(event2)).toThrowError();

    expect(dag.connected_sub_graphs.length.val).toBe(1);
    expect(dag.connected_sub_graphs.get(0).val).toEqual([event1]);

    dag.remove_event(event1);

    // Event 2 is no longer part of the DAG.
    expect(() => dag.get_children(event1)).toThrowError();
    expect(() => dag.get_parents(event1)).toThrowError();

    expect(dag.connected_sub_graphs.length.val).toBe(0);
});

test("Removing an event of which the removal of its edges would result in a disconnected graph should result in a disconnected graph.", () => {
    const dag = new QuestEventDagModel(1);
    const event1 = new QuestEventModel(1, 1, new WaveModel(1, 1, 1), 30, 0);
    const event2 = new QuestEventModel(2, 1, new WaveModel(2, 1, 1), 30, 0);
    const event3 = new QuestEventModel(3, 1, new WaveModel(3, 1, 1), 30, 0);

    // Event 2 connects event 1 to event 3.
    dag.add_event(event1, [], []);
    dag.add_event(event2, [event1], []);
    dag.add_event(event3, [event2], []);

    // Graph is fully connected at this point.
    expect(dag.connected_sub_graphs.length.val).toBe(1);

    dag.remove_event(event2);

    expect(dag.get_children(event1)).toEqual([]);
    expect(dag.get_parents(event1)).toEqual([]);

    // Event 2 is no longer part of the DAG.
    expect(() => dag.get_children(event2)).toThrowError();
    expect(() => dag.get_parents(event2)).toThrowError();

    expect(dag.get_children(event3)).toEqual([]);
    expect(dag.get_parents(event3)).toEqual([]);

    expect(dag.connected_sub_graphs.length.val).toBe(2);
    expect(dag.connected_sub_graphs.get(0).val).toEqual([event1]);
    expect(dag.connected_sub_graphs.get(1).val).toEqual([event3]);
});
