'use strict'

// 개인정보처리 및 이용내역 HTML값을 가져와 돔에 셋팅해주는 스크립트

const privacy = document.querySelector('.privacy');
const service = document.querySelector('.service');

if (privacy) {
  getPrivacyContent();
} else {
  getServiceCountent();
}

function getPrivacyContent() {
  const url = `${window.location.origin}/content/privacy`;
  return fetch(url, {
    method: 'GET'
  })
  .then(response => {
      if (!response.ok) {
          return response.json()
              .catch(() => {
                  // Couldn't parse the JSON
                  throw new Error(response.status);
              })
              .then(({message}) => {
                  // Got valid JSON with error response, use it
                  throw new Error(message || response.status);
              });
      }
      return response.text();
  })
  .then(result => {
    privacy.innerHTML = result;
  });
}

function getServiceCountent() {
  const url = `${window.location.origin}/content/service`;
  return fetch(url, {
    method: 'GET'
  })
  .then(response => {
      if (!response.ok) {
          return response.json()
              .catch(() => {
                  // Couldn't parse the JSON
                  throw new Error(response.status);
              })
              .then(({message}) => {
                  // Got valid JSON with error response, use it
                  throw new Error(message || response.status);
              });
      }
      return response.text();
  })
  .then(result => {
    service.innerHTML = result;
  });
}
