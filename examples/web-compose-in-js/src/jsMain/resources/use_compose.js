// reference for ComposeCounterApp controller
let counterController = undefined;

const showCounterButton = document.getElementById('showCounter');
const container = document.getElementById('container');

showCounterButton.addEventListener('click', () => {
    if (!counterController) {

        // create a div that will serve as a root of Composition
        const divContainerForCounter = document.createElement('div');
        container.appendChild(divContainerForCounter);
        divContainerForCounter.id = 'counterByCompose';

        // create a composition with a root in <div id='counterByCompose'>
        counterController = MyComposables.ComposeCounterApp('counterByCompose', (newCount) => {
            console.log(`Counter was updated. New value = ${newCount}`);
        });

        const randomInitialCount = Math.floor(Math.random() * 1000);
        // Controller can be used to update the composition's state
        counterController.setCount(randomInitialCount);

        showCounterButton.innerText = 'Hide counter (managed by Compose)';
    } else {
        // the composition is not needed anymore. It's necessary to dispose it:
        counterController.dispose();
        counterController = undefined;

        // now we can remove the root of the composition.
        container.removeChild(document.getElementById('counterByCompose'));

        showCounterButton.innerText = 'Show counter (managed by Compose)';
    }
});

