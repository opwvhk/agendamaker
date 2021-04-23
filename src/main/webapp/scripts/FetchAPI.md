# Javascript Fetch API by examples

Below are some examples. Most of these, and additional information, can be found on the
[MDN page describing the Fetch API](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch)


## Basic GET request

```
fetch('http://example.com/movies.json')
  .then(response => response.json())
  .then(data => console.log(data));
```


## Post JSON to read JSON

```
let data = { username: 'example' };
fetch('https://example.com/profile', {
  method: 'POST', // or 'PUT'
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify(data),
})
.then(response => response.json())
.then(data => {
  console.log('Success:', data);
})
.catch((error) => {
  console.error('Error:', error);
});
```


## Upload a file as request body

```
function upload(file) {
  fetch('http://www.example.net', {
    method: 'POST',
    headers: {
      // Content-Type may need to be completely **omitted** or you may need something
      "Content-Type": "You will perhaps need to define a content-type here"
    },
    body: file // This is your file object
  }).then(
    response => response.json() // if the response is a JSON object
  ).then(
    success => console.log(success) // Handle the success response object
  ).catch(
    error => console.log(error) // Handle the error response object
  );
}

// Find the input, and attach an event handler.
let input = document.getElementById('fileinput'); // file object
function onSelectFile() {
  upload(input.files[0]);
}
input.addEventListener('change', onSelectFile, false);
```


## Upload multiple files and form fields

```
let formData = new FormData();
let photos = document.querySelector('input[type="file"][multiple]');

formData.append('title', 'My Vegas Vacation');
for (let i = 0; i < photos.files.length; i++) {
  formData.append('photos', photos.files[i]);
}

fetch('https://example.com/posts', {
  method: 'POST',
  body: formData,
})
.then(response => response.json())
.then(result => {
  console.log('Success:', result);
})
.catch(error => {
  console.error('Error:', error);
});
```



