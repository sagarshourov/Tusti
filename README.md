# Water Sort Puzzle Game 🎮

A beautiful and interactive Water Sort Puzzle game built with Java for Android. Challenge your mind by sorting colored water into test tubes!


## 📱 About The Game

Water Sort Puzzle is a classic puzzle game where you need to sort colored water layers into test tubes. Each tube can hold multiple layers of water, and you can only pour water of the same color into another tube. The goal is to have each tube containing only one color (or empty).

## ✨ Features

- **Multiple Difficulty Levels**: Easy, Medium, Hard, Very Hard, and Impossible!
- **Smooth Animations**: Tubes scale when selected and shake on invalid moves
- **Realistic Pouring Effects**: Watch water flow with animated droplets
- **Move Counter**: Track your progress
- **Win Detection**: Celebrate when you solve the puzzle
- **Restart Option**: Reset the current level anytime
- **Beautiful UI**: Gradient backgrounds and modern material design

## 🎯 How to Play

1. **Select a tube** by tapping on it (must contain water)
2. **Choose destination tube** by tapping another tube
3. **Pour water** if:
   - The destination tube has the same top color OR is empty
   - The destination tube is not full
4. **Win** by having all tubes either empty or filled with a single color



## 🛠️ Technologies Used

- **Java** - Core programming language
- **Android SDK** - Native Android development
- **ConstraintLayout** - Responsive UI layout
- **Animators** - Smooth object animations
- **CardView** - Beautiful tube design
- **GridLayout** - Tube grid arrangement

## 📦 Installation

### Clone the Repository
```bash
git clone https://github.com/sagarshourov/Tusti.git
```

### Open in Android Studio
1. Launch Android Studio
2. Select "Open an existing project"
3. Navigate to the cloned repository
4. Click OK

### Build and Run
1. Connect your Android device or start an emulator
2. Click the Run button (▶) in Android Studio
3. Wait for the build to complete
4. The app will launch on your device

## 🎮 Game Structure

```
app/
├── src/
│   └── main/
│       ├── java/org/example/waterpuzzle/
│       │   └── MainActivity.java      # Main game logic
│       ├── res/
│       │   ├── layout/
│       │   │   └── activity_main.xml  # Main layout
│       │   ├── drawable/               # UI resources
│       │   │   ├── menu_gradient.xml
│       │   │   ├── game_gradient.xml
│       │   │   ├── menu_button_bg.xml
│       │   │   ├── game_button_bg_red.xml
│       │   │   ├── game_button_bg_blue.xml
│       │   │   └── water_drop.xml
│       │   ├── anim/
│       │   │   └── shake.xml          # Shake animation
│       │   └── values/
│       │       ├── strings.xml
│       │       ├── colors.xml
│       │       └── styles.xml
│       └── AndroidManifest.xml
```

## 🎯 Game Rules

- **Pouring Rule**: Can only pour water of the same color
- **Capacity Rule**: Tubes cannot exceed their capacity (4 layers)
- **Empty Tubes**: Can receive any color
- **Win Condition**: All tubes contain only one color or are empty
- **Valid Moves**: Source tube must have water, destination must have space

## 🎨 Color Palette

| Color | Hex Code |
|-------|----------|
| Red | `#FF6347` |
| Blue | `#1E90FF` |
| Yellow | `#FFD700` |
| Green | `#32CD32` |
| Purple | `#9370DB` |
| Light Green | `#90EE90` |
| Light Blue | `#ADD8E6` |
| Orange | `#FF8C00` |
| Brown | `#8B4513` |
| Pink | `#FFB6C1` |

## 🚀 Future Enhancements

- [ ] Add sound effects
- [ ] Implement level editor
- [ ] Add hint system
- [ ] Create daily challenges
- [ ] Add leaderboard
- [ ] Support for more tube capacities
- [ ] Undo/redo functionality
- [ ] Time-based challenges

## 🐛 Known Issues

- None reported yet. Please create an issue if you find any!

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👏 Acknowledgements

- Inspired by classic water sort puzzle games
- Icons and emojis from the Unicode Consortium
- Built with ❤️ for Android

## 📞 Contact

Engr. Sagar Shourov Roy 
Project Link: [https://github.com/sagarshourov/Tusti/](https://github.com/sagarshourov/Tusti/)

---

**Enjoy the game! If you like it, don't forget to ⭐ the repository!**
