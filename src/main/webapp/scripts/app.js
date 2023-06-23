/*
 * Downloaded on 2023-06-23 using documentation from https://vuejs.org/guide/quick-start.html#using-vue-from-cdn
 */
import {createApp} from './unpkg.com_vue@3.3.4_dist_vue.esm-browser.js';
import Message from './components/message.js';

const cookies = {
	get(/*String*/name) {
		const nameEQ = name + "=";
		const cookieArray = document.cookie.split(';');
		for (let i = 0; i < cookieArray.length; i++) {
			let cookie = cookieArray[i];
			while (cookie.charAt(0) === ' ') cookie = cookie.substring(1, cookie.length);
			if (cookie.indexOf(nameEQ) === 0) {
				const cookieValue = cookie.substring(nameEQ.length, cookie.length);
				console.log("Read cookie '" + name + "'; its value is: " + cookieValue);
				return cookieValue;
			}
		}
		return null;
	},
	set(/*String*/name, /*String*/value, /*int*/days = 0) {
		let expires = "; Expires=Thu, 01 Jan 1970 00:00:01 GMT";
		if (days > 0) {
			const date = new Date();
			date.setTime(date.getTime() + (days * 86400000));
			expires = "; Expires=" + date.toUTCString();
		}
		const cookieValue = (value || "");
		console.log("Setting cookie '" + name + "' to " + cookieValue);
		document.cookie = name + "=" + cookieValue + expires + "; Path=/; SameSite=Strict";
	},
	remove(name) {
		this.set(name, "", 0);
	}
}

//noinspection JSUnusedGlobalSymbols
let app = createApp({
	components: {
		Message
	},
	data() {
		return {
			showDateTitleTableExplanation: true,
			messages: [],
			title: "",
			subtitle: "",
			timeTablePages: 3,
			notesPages: 1,
			mindmapPages: 2,
			dateTitles: [
				{date: "2023-09-04", text: ""},
				{date: "2023-10-02", text: "Projectweek 1"},
				{date: "2023-10-07", text: ""},
				{date: "2023-10-23", text: "Herfstvakantie"},
				{date: "2023-10-30", text: ""},
				{date: "2023-12-25", text: "Kerstvakantie"},
				{date: "2024-01-08", text: ""},
				{date: "2024-02-19", text: "Voorjaarsvakantie"},
				{date: "2024-02-26", text: ""},
				{date: "2024-04-01", text: "2e Paasdag"},
				{date: "2024-04-02", text: "Projectweek 2"},
				{date: "2024-04-07", text: ""},
				{date: "2024-04-22", text: "Meivakantie"},
				{date: "2024-05-06", text: ""},
				{date: "2024-05-09", text: "Hemelvaart"},
				{date: "2024-05-10", text: "dag na Hemelvaart (vrij)"},
				{date: "2024-05-11", text: ""},
				{date: "2024-05-20", text: "2e Pinksterdag"},
				{date: "2024-05-21", text: ""},
				{date: "2024-07-01", text: "Toetsweek"},
				{date: "2024-07-06", text: ""},
				{date: "2024-07-20", text: "Zomervakantie"}
			],
			allInputsAreValid: false
		};
	},
	computed: {
		hasNotesPages() {
			//noinspection JSUnresolvedVariable
			return this.notesPages > 0;
		}
	},
	mounted() {
		//noinspection JSUnresolvedVariable
		this.$refs.dateTitleTableExplanation.open = cookies.get("explainDateTitleTable") !== "false";
		this.checkInputs();
	},
	methods: {
		toggleDateTitleTableExplanation() {
			//noinspection JSUnresolvedVariable
			const element = this.$refs.dateTitleTableExplanation;
			const nextOpenValue = !element.open; // The browser will toggle this element.
			cookies.set("explainDateTitleTable", nextOpenValue ? "true" : "false", 7); // Keep preference for 7 days
		},
		checkInputs() {
			const allInputs = document.getElementsByTagName('input');
			for (let inputField of allInputs) {
				if (!inputField.validity.valid) {
					this.allInputsAreValid = false;
					return true;
				}
			}
			this.allInputsAreValid = true;
			return true;
		},
		sortDateTitles() {
			this.dateTitles = this.dateTitles.sort((dt1, dt2) => compare(dt1.date, dt2.date));
		},
		removeDateTitle(dateTitles, dateTitle) {
			dateTitles.splice(dateTitles.indexOf(dateTitle), 1);
		},
		addDateTitle(dateTitles) {
			dateTitles.push({});
		},
		generatePlanner() {
			// noinspection JSUnresolvedReference: Vue actually changes the value of 'this' to the model (data() updated with the input values)
			const options = {
				method: 'POST',
				body: JSON.stringify({
					title: this.title,
					subtitle: this.subtitle,
					timeTablePages: this.timeTablePages,
					notesPages: this.notesPages,
					mindmapPages: this.mindmapPages,
					startDate: this.startDate,
					endDate: this.endDate,
					dateTitles: this.dateTitles
				}),
				headers: {
					'Content-Type': 'application/json'
				}
			};
			goFetch('rest', options,
					response => {
						const filename = determineFilename(response.headers, 'planagenda.pdf');
						return response.blob().then(blob => downloadBlob(blob, filename));
					},
					json => json.text().then(error => this.messages.push(error)),
					error => console.log(error)
			);
		}
	}
});
app.mount('body');

function compare(o1, o2) {
	return o1 < o2 ? -1 : o1 > o2 ? 1 : 0;
}

/**
 * Use the Fetch API using the specified URL and options. Upon success and failure, the specified success resp. error handler is called.
 * If either fail or if the request/response failed, the generic error handler is called.
 *
 * The success and failure handlers are called to handle a Promise, and receive the response object.
 *
 * @param url {string} the URL to call
 * @param options {*} the options to use
 * @param successHandler
 * @param errorHandler
 * @param genericErrorHandler
 */
function goFetch(url, options, successHandler, errorHandler, genericErrorHandler) {
	if (errorHandler == null) {
		errorHandler = json => {
			console.log(json);
		};
	}
	if (genericErrorHandler == null) {
		genericErrorHandler = err => {
			console.log(err);
		};
	}
	fetch(url, options).then(response => {
		return response.ok ? successHandler(response) : errorHandler(response);
	}).catch(genericErrorHandler);
}

function determineFilename(/*Headers*/headers, /*String*/defaultFilename) {
	let filename = defaultFilename;
	const disposition = headers.get('Content-Disposition');
	if (disposition && disposition.indexOf('attachment') !== -1) {
		const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
		const matches = filenameRegex.exec(disposition);
		if (matches != null && matches[1]) {
			filename = matches[1].replace(/['"]/g, '');
		}
	}
	return filename;
}

function downloadBlob(/*Blob*/blob, /*String*/filename) {
	const dataUrl = URL.createObjectURL(blob);
	try {
		const downloadLink = document.createElement('a');
		downloadLink.href = dataUrl;
		downloadLink.download = filename;
		//downloadLink.style.position = 'absolute';
		//downloadLink.style.left = '-9999px';
		//document.body.appendChild(downloadLink);
		downloadLink.click();
		//document.body.removeChild(downloadLink);
	} finally {
		setTimeout(() => {
			// Release the object URL after the browser has time to start the download.
			URL.revokeObjectURL(dataUrl);
		}, 250);
	}
}
