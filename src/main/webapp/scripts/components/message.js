export default {
	name: 'Message',
	props: ['modelValue'],
	template: `
		<transition name="popup">
			<aside class="message" v-if="modelValue.length > 0" @click="modelValue.shift()">{{modelValue[0]}}</aside>
		</transition>
	`
};
