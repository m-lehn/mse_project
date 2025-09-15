# 🍄 Mushroom Identification App (Android)

This project is a **proof-of-concept Android app** for offline mushroom genus classification using a lightweight neural network deployed directly on a mobile device.  

The main goal was to **experiment with on-device inference** and explore the challenges of deploying deep learning models in mobile applications — without requiring an internet connection.

---

## ✨ Features

- 📱 Android app with a simple user interface  
- ⚡ Runs completely offline — no internet connection needed  
- 🧠 Pretrained **ResNet-50** backbone (Hugging Face) with a custom classification head  
- 🍄 Identifies images as one of **9 mushroom genera** (from Northern Europe)  
- 🔧 Experiment in **on-device ML deployment** and performance testing  

---

## 📊 Dataset

The model was trained on the [Mushrooms Classification (Common Genus) dataset](https://www.kaggle.com/datasets/maysee/mushrooms-classification-common-genuss-images).  

- **9 folders**, each representing a mushroom genus common in Northern Europe  
- Between **300–1500 images per genus**  
- Labels = folder names  

⚠️ **Important note:**  
This app only classifies mushrooms at the **genus level** and should **not be used for real-world identification**. Mushroom identification is highly complex, and eating misidentified mushrooms can be **extremely dangerous**.  
This project is for **educational and experimental purposes only**.

---

## 🚀 Technical Overview

- **Frameworks:** Hugging Face, PyTorch, TensorFlow Lite (for mobile deployment)  
- **Model:** ResNet-50 backbone + small dense classification layer  
- **Deployment:** Converted for mobile inference, integrated into a basic Android app  

---

## 📱 App Preview

👉 *(Placeholder for screenshots — add them here!)*  
- [ ] Add app UI screenshot  
- [ ] Add sample classification result screenshot  

---

## 🕹️ Usage

1. Launch the app on your Android device.  
2. Take a photo of a mushroom (or load one from gallery).  
3. The model predicts the most likely **genus** (out of 9 classes).  

---

## 🙈 "Dark Pattern" Experiment

The app includes a **placeholder roadblock screen** (e.g., suggesting support via Patreon or coffee donation).  
This was included as a design experiment in user experience, but **no real Patreon link exists**.  

---

## 🎯 Purpose of the Project

This project was not intended as a production-ready mushroom identification tool. Instead, the objectives were:  

- Learn and practice **model deployment to mobile devices**  
- Explore the balance between **model accuracy, size, and performance**  
- Prototype a minimal Android app around an ML model  
- Have fun with a practical but deliberately limited use case  

---

## 📷 Screenshots

*(Add some pictures here!)*  
- App UI  
- Example classification  

---

## ⚠️ Disclaimer

This app is for **educational purposes only**.  
Do **not** use it for real mushroom identification.  
Always consult expert sources before consuming wild mushrooms.  

---

## 📚 License

[MIT License](LICENSE)  
Free to use, modify, and learn from.
