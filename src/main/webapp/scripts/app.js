//import { createApp } from './vue.v3.0.11.esm.js';
import {createApp} from './vue.v3.0.11.esm.min.js';
import Message from './components/message.js';

//import {DateTime, Settings} from './luxon.v1.26.0.esm.js';
//Settings.defaultLocale = 'nl';

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
			messages: ['Foo'],
			title: "",
			subtitle: "",
			notesPages: 1,
			dateTitles: [
				{date: "", text: ""}
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
			for(let inputField of allInputs) {
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
			const options = {
				method: 'POST',
				body: JSON.stringify({
					title: this.title,
					subtitle: this.subtitle,
					notesPages: this.notesPages,
					dateTitles: this.dateTitles
				}),
				headers: {
					'Content-Type': 'application/json'
				}
			};
			goFetch('rest', options, response => {
				const filename = determineFilename(response.headers, 'planagenda.pdf');
				return response.blob().then(blob => downloadBlob(blob, filename));
			}, response => response.text().then(error => this.messages.push(error)));
		}
	}
});
app.mount('body');

//function formatDate(dateStr) {
//	return dateStr.length > 0 ? DateTime.fromISO(dateStr).toLocaleString(DateTime.DATE_FULL) : '';
//}

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
